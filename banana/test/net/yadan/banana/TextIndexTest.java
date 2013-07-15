package net.yadan.banana;

import net.yadan.banana.map.IVarKeyHashMap;
import net.yadan.banana.map.VarKeyHashMapVisitor;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TextIndexTest {

  private char SEPS[] = { ' ', '\t', '.', ',', ';', '/', '\\', ':' };

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testIndex() {
    String words[] = {"The", "quick", "brown", "fox", "jumps", "over", "the", "lazy" , "dog"};
    String text = "The, quick brown fox jumps ,over: the lazy dog...";
    TextIndex index = new TextIndex(100, 30);
    index.setDebug(true);
    index.index(99, text, SEPS);
    assertEquals(index.getNumWords(), 8);
    for(String word : words) {
      assertArrayEquals(new int[] {99}, index.find(word));
    }

    //index.getWord2DocList().visitRecords(new KeyPrinter());
  }

  @Test
  public void testMultiDoc() {
    String texts[] =  {
        "A B C",
        "A C E",
        "E B D",
    };

    Object res[][] = {
        { "A", 0, 1 },
        { "B", 0, 2 },
        { "C", 0 },
        { "D", 2},
        { "E", 1, 2},
    };

    TextIndex index = new TextIndex(100, 30);
    for (int i = 0; i < texts.length; i++) {
      index.index(i, texts[i], SEPS);
    }

    for (int i = 0; i < res.length; i++) {
      Object r[] = res[i];
      String word = (String) r[0];
      int[] docs = index.find(word);
      for(int k=0;k<r.length - 1; k++) {
        assertEquals(r[k + 1], docs[k]);
      }
    }
  }

  @Test
  public void testDocListResize() {
    TextIndex index = new TextIndex(4, 30);
    index.getWord2DocList().setDebug(DebugLevel.DEBUG_STRUCTURE);
    int expected[] = new int[7];
    for (int i = 0; i < 7; i++) {
      expected[i] = i*i;
      index.index(i*i, "Hello world", SEPS);
    }

    int[] find = index.find("Hello");
    assertArrayEquals(expected, find);


//    index.getWord2DocList().visitRecords(new KeyPrinter());
  }

  static final class KeyPrinter implements VarKeyHashMapVisitor {
    IBuffer m_tmpWord = new Buffer(10);
    @Override
    public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total) {
      int wordSize = map.keysMemory().getInt(keyPtr, 0) * 2;
      char chars[] = new char[wordSize];
      map.keysMemory().getBuffer(keyPtr, 1, m_tmpWord, wordSize);
      m_tmpWord.getChars(0, chars, 0, wordSize);
      System.out.println(new String(chars));
    }

    @Override
    public void end(IVarKeyHashMap map) {
    }

    @Override
    public void begin(IVarKeyHashMap map) {
    }
  }
}
