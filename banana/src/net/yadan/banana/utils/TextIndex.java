/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.utils;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.map.IVarKeyHashMap;
import net.yadan.banana.map.VarKeyHashMap;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.malloc.MultiSizeAllocator;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TextIndex {

  private static final int DOC_LIST_BLOCK_SIZE = 10;
  private static final int INITIAL_DOC_LIST_SIZE = DOC_LIST_BLOCK_SIZE
      - VarKeyHashMap.RESERVED_SIZE;
  public static int DOC_LIST_ALLOCATION_SIZE_OFFSET = 0;
  public static int DOC_LIST_SIZE_OFFSET = 1;
  private static int DOC_LIST_DATA_OFFSET = 2;
  // private static double DOC_LIST_GROWTH_FACTOR = 1.2;

  private int MAX_WORD_LENGTH = 30;

  private int m_maxDocListSize = -1;
  private char[] m_wordBuf;
  private int m_wordLength;
  int m_textOffset;

  IBuffer m_keyBuffer;
  private IVarKeyHashMap m_word2DocList;
  private IVarKeyHashMap m_currentDocumentWords; // TODO: should be a Set
  private IVarKeyHashMap m_stopWords; // TODO: should be a Set

  // stats
  private long m_numDocumentsIndexed = 0;
  private long m_numWordsTokenized = 0;
  private long m_totalIndexedTextSize = 0;

  public TextIndex(int initialWordsCapacity, int maxWordLength) {
    m_wordBuf = new char[MAX_WORD_LENGTH];
    m_wordLength = 0;
    m_textOffset = 0;

    IMemAllocator docListsMemory = new TreeAllocator(initialWordsCapacity, DOC_LIST_BLOCK_SIZE, 2.0);
    IMemAllocator keys = new MultiSizeAllocator(1024, new int[] { 1, 2, 4, 8, 16, 32 }, 1.5);
    m_word2DocList = new VarKeyHashMap(docListsMemory, keys, initialWordsCapacity, 0.75);
    m_keyBuffer = new Buffer(50);
    m_currentDocumentWords = new VarKeyHashMap(docListsMemory, keys, initialWordsCapacity, 0.75);
    m_stopWords = new VarKeyHashMap(docListsMemory, keys, 100, 0.75);
  }

  public int index(int documentId, String text, char seps[]) {
    m_currentDocumentWords.clear();
    m_numDocumentsIndexed++;
    m_totalIndexedTextSize += text.length();
    int length = text.length();
    char line[] = text.toCharArray();
    m_textOffset = 0;
    int numWords = 0;
    while (m_textOffset < length) {
      nextWord(line, length, seps);
      if (m_wordLength > 0) {
        try {
          numWords++;
          m_numWordsTokenized++;
          m_keyBuffer.reset();
          // m_keyBuffer.appendInt(m_wordLength);
          m_keyBuffer.appendChars(m_wordBuf, 0, m_wordLength);

          if (m_stopWords.containsKey(m_keyBuffer)) {
            continue;
          }

          if (m_currentDocumentWords.containsKey(m_keyBuffer)) {
            continue;
          }
          m_currentDocumentWords.createRecord(m_keyBuffer, 0);

          int docListRecord = m_word2DocList.findRecord(m_keyBuffer);
          if (docListRecord == -1) {
            // new word, create a list of documents this word is in]
            docListRecord = m_word2DocList.createRecord(m_keyBuffer, INITIAL_DOC_LIST_SIZE);
            m_word2DocList.setInt(docListRecord, DOC_LIST_ALLOCATION_SIZE_OFFSET,
                INITIAL_DOC_LIST_SIZE);
            m_word2DocList.setInt(docListRecord, DOC_LIST_SIZE_OFFSET, 0); // zero
          }

          int size = m_word2DocList.getInt(docListRecord, DOC_LIST_SIZE_OFFSET);
          int maxCap = m_word2DocList.getInt(docListRecord, DOC_LIST_ALLOCATION_SIZE_OFFSET);
          // this word appears too many times, stop keeping track of it
          if (m_maxDocListSize != -1 && size >= m_maxDocListSize) {
            continue;
          }

          if (size > maxCap - (VarKeyHashMap.RESERVED_SIZE + DOC_LIST_DATA_OFFSET)) {
            // System.out.println(m_word2DocList.valueMemory().pointerDebugString(docListRecord));

            int newSize = maxCap + DOC_LIST_BLOCK_SIZE;
            docListRecord = m_word2DocList.reallocRecord(m_keyBuffer, newSize);
            m_word2DocList.setInt(docListRecord, DOC_LIST_ALLOCATION_SIZE_OFFSET, newSize);
            // System.out.println(m_word2DocList.valueMemory().pointerDebugString(docListRecord));
          }

          m_word2DocList.setInt(docListRecord, DOC_LIST_SIZE_OFFSET, size + 1);
          m_word2DocList.setInt(docListRecord, DOC_LIST_DATA_OFFSET + size, documentId);
        } catch (RuntimeException e) {
          System.out.println("Error indexing document " + documentId + " , word : "
              + new String(m_wordBuf, 0, m_wordLength));
          throw e;
        }
      }
    }
    return numWords;
  }

  int EMPTY[] = new int[0];

  public int[] find(String word) {
    m_keyBuffer.reset();
    // m_keyBuffer.appendInt(word.length());
    m_keyBuffer.appendChars(word.toLowerCase().toCharArray());
    int docListRecord = m_word2DocList.findRecord(m_keyBuffer);
    if (docListRecord == -1) {
      return new int[0];
    } else {

      int size = m_word2DocList.getInt(docListRecord, DOC_LIST_SIZE_OFFSET);
      int res[] = new int[size];
      m_word2DocList.getInts(docListRecord, DOC_LIST_DATA_OFFSET, res, 0, size);
      return res;
    }
  }

  public int getNumWords() {
    return m_word2DocList.size();
  }

  private void nextWord(char line[], int length, char[] seps) {
    m_wordLength = 0;
    while (m_textOffset < length) {
      char c = line[m_textOffset++];
      boolean found_sp = false;
      for (int j = 0; j < seps.length; j++) {
        if (c == seps[j]) {
          found_sp = true;
          break;
        }
      }
      if (found_sp) {
        if (m_wordLength > 0) {
          break;
        } else {
          // if no word yet, keep looking
          continue;
        }
      }

      if (m_wordLength < MAX_WORD_LENGTH) {
        m_wordBuf[m_wordLength++] = Character.toLowerCase(c);
      }
    }
  }

  public IVarKeyHashMap getWord2DocList() {
    return m_word2DocList;
  }

  public long computeMemoryUsage() {
    return m_word2DocList.computeMemoryUsage();
  }

  public long getNumDocumentsIndexed() {
    return m_numDocumentsIndexed;
  }

  public long getNumWordsTokenized() {
    return m_numWordsTokenized;
  }

  public long getTotalIndexedTextSize() {
    return m_totalIndexedTextSize;
  }

  public void setDebug(boolean b) {
    m_word2DocList.setDebug(DebugLevel.DEBUG_CONTENT);
    m_currentDocumentWords.setDebug(DebugLevel.DEBUG_STRUCTURE);
    m_stopWords.setDebug(DebugLevel.DEBUG_CONTENT);
  }

  public void addStopWord(String word) {
    m_keyBuffer.reset();
    m_keyBuffer.appendChars(word.toLowerCase().toCharArray());
    m_stopWords.createRecord(m_keyBuffer, 0);
  }

  public void setMaxDocListSize(int maxDocListSize) {
    m_maxDocListSize = maxDocListSize;
  }
}
