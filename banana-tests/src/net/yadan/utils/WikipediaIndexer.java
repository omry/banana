package net.yadan.utils;

import net.yadan.banana.TextIndex;
import net.yadan.banana.map.IVarKeyHashMap;
import net.yadan.banana.map.VarKeyHashMapVisitorAdapter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;


public class WikipediaIndexer {

  private static char SEPS[] = " \t,.;:/\\{}[]()<>'\"=|\n!-_*?&0123456789–".toCharArray();
  // http://en.wikipedia.org/wiki/Stop_words
  private static String m_stopWords;
  static {
    m_stopWords = "a,able,about,across,after,all,almost,also,am,among,an,"
        + "and,any,are,as,at,be,because,been,but,by,can,cannot,could,dear,did,do,does,either,"
        + "else,ever,every,for,from,get,got,had,has,have,he,her,hers,him,his,how,"
        + "however,i,if,in,into,is,it,its,just,least,let,like,likely,may,me,might,"
        + "most,must,my,neither,no,nor,not,of,off,often,on,only,or,other,our,own,"
        + "rather,said,say,says,she,should,since,so,some,than,that,the,their,"
        + "them,then,there,these,they,this,tis,to,too,twas,us,wants,was,we,were,"
        + "what,when,where,which,while,who,whom,why,will,with,would,yet,you,your";

    m_stopWords += "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,"
        + "one,two,three,four,five,six,seven,eight,nine,ten,"
        + "www,http,html,com,org,net,url,jpg,gif,thumb,history,page,image,"
        + "see,ref,references,name,date,external,well,used,being,use,later,man,over,last,such,"
        + "new,year,title,right,left,called,many,category,first,between,"
        + "cite,reflist,links,known,publisher,more,people,place,same,id,end,number,states,several,state,"
        + "book,early,made,both,book,isbn,world,web,during,up,file,including,th,part,work,accessdate,"
        + "years,time,under,";
  }

  static long total_time = 0;
  static RateCounter m_documentsRate = new RateCounter(10);
  static RateCounter m_wordsRate = new RateCounter(10);
  static int m_numIndexed = 0;
  private static TextIndex s_index;

  public static void main(String[] args) throws ParserConfigurationException, SAXException,
      IOException {
    String xmlFile = args[0];
    // TODO : change initial index size
    s_index = new TextIndex(100, 30);
    s_index.setMaxDocListSize(Integer.MAX_VALUE);
    s_index.setDebug(true);

    StringTokenizer t = new StringTokenizer(m_stopWords, ",");
    while (t.hasMoreTokens()) {
      s_index.addStopWord(t.nextToken());
    }

    // if (true) {
    // indexText(1, test2);
    // System.exit(0);
    // }

    System.out.println("Indexing " + xmlFile);

    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();

    DefaultHandler handler = new DefaultHandler() {

      boolean inPage = false;
      boolean inPageId = false;
      boolean inPageTitle = false;
      boolean inPageRevision = false;
      boolean inPageRevisionText = false;

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
        if (!inPage && "page".equalsIgnoreCase(qName)) {
          inPage = true;
        }

        if (inPage) {

          if (!inPageTitle && "title".equalsIgnoreCase(qName)) {
            inPageTitle = true;
          }

          if (!inPageId && "id".equalsIgnoreCase(qName)) {
            inPageId = true;
          }

          if (!inPageRevision && "revision".equalsIgnoreCase(qName)) {
            inPageRevision = true;
          }
        }

        if (inPageRevision) {
          if (!inPageRevisionText && "text".equalsIgnoreCase(qName)) {
            inPageRevisionText = true;
          }
        }

      }

      StringBuilder m_currentText = new StringBuilder();
      private int m_currentID;

      // private String m_currentTitle;

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {

        if (inPageRevision) {
          if (inPageRevisionText && "text".equalsIgnoreCase(qName)) {
            inPageRevisionText = false;
          }
        }

        if (inPage) {
          if (inPageRevision && "revision".equalsIgnoreCase(qName)) {

            String text = m_currentText.toString();
            if (!text.startsWith("#REDIRECT") && !text.startsWith("{{Redirect")) {
              indexText(m_currentID, text);
            }

            m_currentText.setLength(0);
            inPageRevision = false;
          }

          if (inPageTitle && "title".equalsIgnoreCase(qName)) {
            inPageTitle = false;
          }

          if (inPageId && "id".equalsIgnoreCase(qName)) {
            inPageId = false;
          }

        }

        if (inPage && "page".equalsIgnoreCase(qName)) {
          inPage = false;
        }

      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        if (inPageTitle) {
          // m_currentTitle = new String(ch, start, length);
        }
        if (inPageRevisionText) {
          m_currentText.append(ch, start, length);
        }

        if (inPageId) {
          String string = new String(ch, start, length);
          m_currentID = Integer.parseInt(string);
        }
      }
    };

    saxParser.parse(new File(xmlFile), handler);

  }

  public static void indexText(int documentId, String text) {
    m_documentsRate.tick();
    long t = System.currentTimeMillis();
    int numWords = s_index.index(documentId, text, SEPS);
    m_wordsRate.tick(numWords);
    total_time += (System.currentTimeMillis() - t);
    if (m_numIndexed++ % 10000 == 0 && m_documentsRate.getTicksPerSecond() != -1) {
      printStats(s_index);
    }
  }

  public static void printStats(TextIndex index) {
    System.out.println("Indexing " + m_documentsRate.getTicksPerSecond() + "/sec, "
        + m_wordsRate.getTicksPerSecond() + " words/sec");

    System.out.println(String.format("Indexed %s documents (with %s words), total of %s",
        Util.formatNum(index.getNumDocumentsIndexed()),
        Util.formatNum(index.getNumWordsTokenized()),
        Util.formatSize(index.getTotalIndexedTextSize())));

    long memoryUsage = index.computeMemoryUsage();
    IVarKeyHashMap word2Index = index.getWord2DocList();

    MaxDocListFinder visitor = new MaxDocListFinder();
    word2Index.visitRecords(visitor);

    final Histogram docListLengthHistogram = Histogram.createStatic(10, visitor.min, visitor.max);
    IndexStatsCollector collector = new IndexStatsCollector(docListLengthHistogram);
    word2Index.visitRecords(collector);

    System.out
        .println(String.format("Index size %s, index/text size=%.2f%%",
            Util.formatSize(memoryUsage),
            100 * (memoryUsage / (float) index.getTotalIndexedTextSize())));

    System.out.println(String.format("\tdoclists : used+free=total : %s+%s=%s",
        Util.formatSize(collector.totalUsedForDocLists * 4),
        Util.formatSize(collector.totalFreeForDocLists * 4),
        Util.formatSize(collector.totalUsedForDocLists * 4 + collector.totalFreeForDocLists * 4)));
    System.out.println(String.format("\twords : used+free=total : %s+%s=%s",
        Util.formatSize(collector.totalUsedForWords * 4),
        Util.formatSize(collector.totalFreeForWords * 4),
        Util.formatSize(collector.totalUsedForWords * 4 + collector.totalFreeForWords * 4)));

    System.out.println("Doc list sizes histogram " + docListLengthHistogram);
    System.out.println();

    int n = 0;
    if (n > 0) {
      TopXWordsCollector topX = new TopXWordsCollector(n);
      word2Index.visitRecords(topX);
      for (WordAndCount w : topX.m_bestWords) {
        System.out.println("'" + w.word + "' : " + w.count);
      }
    }
  }

  private static final class IndexStatsCollector extends VarKeyHashMapVisitorAdapter {
    int totalUsedForWords = 0;
    int totalFreeForWords = 0;
    int totalUsedForDocLists = 0;
    int totalFreeForDocLists = 0;
    private Histogram m_docListLengthHistogram;

    public IndexStatsCollector(Histogram docListLengthHistogram) {
      m_docListLengthHistogram = docListLengthHistogram;
    }

    @Override
    public void visit(IVarKeyHashMap map, int keyPtr, int valuesPtr, long num, long total) {
      int docListSize = map.getInt(valuesPtr, TextIndex.DOC_LIST_SIZE_OFFSET);
      m_docListLengthHistogram.addToHistogram(docListSize);
      int docListAllocation = map.valueMemory().maximumCapacityFor(valuesPtr);
      totalUsedForDocLists += docListSize;
      assert docListAllocation >= docListSize;
      totalFreeForDocLists += (docListAllocation - docListSize);
    }
  }

  private static class WordAndCount implements Comparable<WordAndCount> {
    String word;
    long count;

    public WordAndCount(String w, int c) {
      word = w;
      count = c;
    }

    @Override
    public String toString() {
      return word + ":" + count;
    }

    @Override
    public int compareTo(WordAndCount o) {
      return (int) (count - o.count);
      // return count > o.count ? 1 : count < o.count ? -1 : 0;
    }
  }

  private static final class TopXWordsCollector extends VarKeyHashMapVisitorAdapter {

    PriorityQueue<WordAndCount> m_bestWords;
    private int m_topX;
    IBuffer m_tmpWord = new Buffer(50);
    char chars[] = new char[50];

    public TopXWordsCollector(int topX) {
      if (topX > 0) {
        m_topX = topX;
        m_bestWords = new PriorityQueue<WordAndCount>(topX);
      } else {
        m_topX = -topX;
        m_bestWords = new PriorityQueue<WordAndCount>(-topX, new Comparator<WordAndCount>() {

          @Override
          public int compare(WordAndCount o1, WordAndCount o2) {
            return -o1.compareTo(o2);
          }
        });
      }
    }

    @Override
    public void visit(IVarKeyHashMap map, int keyPtr, int valuesPtr, long num, long total) {
      int wordSize = map.keysMemory().getInt(keyPtr, 0) * 2;
      map.keysMemory().getBuffer(keyPtr, 1, m_tmpWord, wordSize);
      m_tmpWord.getChars(0, chars, 0, wordSize);
      if (chars[wordSize - 1] == 0) {
        wordSize--;
      }

      int docListSize = map.getInt(valuesPtr, TextIndex.DOC_LIST_SIZE_OFFSET);
      String word = new String(chars, 0, wordSize);

      m_bestWords.add(new WordAndCount(word, docListSize));
      if (m_bestWords.size() > m_topX) {
        m_bestWords.remove();
      }
      m_tmpWord.reset();
    }
  }

  private static final class MaxDocListFinder extends VarKeyHashMapVisitorAdapter {
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;

    @Override
    public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total) {
      int size = map.getInt(valuePtr, TextIndex.DOC_LIST_SIZE_OFFSET);
      max = Math.max(size, max);
      min = Math.min(size, min);
    }
  }

  static String test = "[[File:Autistic-sweetiepie-boy-with-ducksinarow.jpg|thumb|alt=Young boy asleep on a bed, facing the camera, with only the head visible and the body off-camera. On the bed behind the boy's head is a dozen or so toys carefully arranged in a line."
      + "|A young boy with autism who has arranged his toys in a row]] '''[[Stereotypy]]''' is repetitive movement, such as hand flapping, head rolling, or body rocking [[Compulsive behavior]]''' is intended and appears to follow rules, such as arranging objects in "
      + "stacks or lines.'''Sameness''' is resistance to change; for example, insisting that the furniture not be moved or refusing to be interrupted. '''[[Ritual#Psychology|Ritualistic behavior]]''' involves an unvarying pattern of daily activities, such as an "
      + "unchanging menu or a dressing ritual. This is closely associated with sameness and an independent validation has suggested combining the two factors.<ref name=Lam-Aman>{{vcite journal |journal=J Autism Dev Disord |year=2007 |volume=37 |issue=5 "
      + "|pages=855–66 |title=The Repetitive Behavior Scale-Revised: independent validation in individuals with autism spectrum disorders |author=Lam KSL, Aman MG |doi=10.1007/s10803-006-0213-z |pmid=17048092 }}</ref>  '''Restricted behavior''' is limited "
      + "in focus, interest, or activity, such as preoccupation with a single television program, toy, or game. '''[[Self-injury]]''' includes movements that injure or can injure the person, such as eye poking, [[skin picking]], hand biting, "
      + "and head banging.<ref name=Johnson/> A 2007 study reported that self-injury at some point affected about 30% of children with ASD.<ref name=Dominick/> No single repetitive or self-injurious behavior seems to be specific to autism, but only"
      + " autism appears to have an elevated pattern of occurrence and severity of these behaviors.<ref>{{vcite journal |journal=J Autism Dev Disord |year=2000 |volume=30 |issue=3 |pages=237–43 |title=Varieties of repetitive behavior "
      + "in autism: comparisons to mental retardation |author=Bodfish JW, Symons FJ, Parker DE, Lewis MH |doi=10.1023/A:1005596502855 |pmid=11055459 }}</ref>";

  static String test2 = "[[File:Autistic-sweetiepie-boy-with-ducksinarow.jpg|thumb|alt=Young boy asleep on a bed, facing the camera, with only the head visible and the body off-camera. On the bed behind the boy's head is a dozen or so toys carefully arranged in a line."
      + "|A young boy with autism who has arranged his toys in a row]] '''[[Stereotypy]]''' is repetitive movement, such as hand flapping, head rolling, or body rocking [[Compulsive behavior]]''' is intended and appears to follow rules, such as arranging objects in "
      + "stacks or lines.'''Sameness''' is resistance to change; for example, insisting that the furniture not be moved or refusing to be interrupted. '''[[Ritual#Psychology|Ritualistic behavior]]''' involves an unvarying pattern of daily activities, such as an "
      + "unchanging menu or a dressing ritual. This is closely associated with sameness and an independent validation has suggested combining the two factors.<ref name=Lam-Aman>{{vcite journal |journal=J Autism Dev Disord |year=2007 |volume=37 |issue=5 "
      + "|pages=855–66 |title=The Repetitive Behavior Scale-Revised: independent validation in individuals with autism spectrum disorders |author=Lam KSL, Aman MG |doi=10.1007/s10803-006-0213-z |pmid=17048092 }}</ref>  '''Restricted behavior''' is limited "
      + "in focus, interest, or activity";
}
