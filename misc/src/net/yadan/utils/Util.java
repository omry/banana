/*
 * Copyright (C) ${year} Omry Yadan <${email}>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by omry at Jun 16, 2008
 */
public class Util {

  // Implementing Fisher–Yates shuffle
  public static void shuffleArray(int[] ar) {
    Random rnd = new Random();
    for (int i = ar.length - 1; i >= 0; i--) {
      int index = rnd.nextInt(i + 1);
      // Simple swap
      int a = ar[index];
      ar[index] = ar[i];
      ar[i] = a;
    }
  }

  public static boolean isInteger(String s) {
    try {
      if (s == null)
        return false;
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isNumeric(String s) {

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9')
        return false;
    }
    return true;
  }

  public static boolean isLong(String s) {
    try {
      if (s == null)
        return false;
      Long.parseLong(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static String implode(Object set[]) {
    return implode(set, ",");
  }

  public static String implode(Object set[], String sep) {
    StringBuffer b = new StringBuffer(set.length * 10);

    if (set.length > 0)
      b.append(set[0]);
    for (int i = 1; i < set.length; i++) {
      b.append(sep).append(set[i]);
    }
    return b.toString();

  }

  public static String implode(Collection<?> set) {
    return implode(set, ",", new DefaultStringConverter());
  }

  public static String implode(String sts[], String sep, StringConverter converter) {
    return implode(Arrays.asList(sts), sep, converter);
  }

  public static String implode(Collection<?> set, String sep, StringConverter converter) {
    Iterator<?> iter = set.iterator();
    StringBuffer b = new StringBuffer(set.size() * 10);
    if (set.size() > 0)
      b.append(converter.toString(iter.next()));

    while (iter.hasNext()) {
      b.append(sep).append(converter.toString(iter.next()));
    }
    return b.toString();
  }

  public static String implodeLongs(Collection<Long> set, String sep) {
    Iterator<Long> iter = set.iterator();
    StringBuffer b = new StringBuffer(set.size() * 10);
    if (set.size() > 0)
      b.append(iter.next());

    while (iter.hasNext()) {
      b.append(sep).append(iter.next());
    }
    return b.toString();
  }

  public static String implode(int[] ii, String sep) {
    StringBuffer b = new StringBuffer(ii.length * 4);
    if (ii.length > 0)
      b.append(ii[0]);

    for (int i = 1; i < ii.length; i++) {
      b.append(sep).append(ii[i]);
    }
    return b.toString();
  }

  public static List<String> getSorted(Map<?, ?> p) {
    List<String> list = new ArrayList<String>(p.size());
    for (Object key : p.keySet()) {
      list.add(key + " : " + p.get(key));
    }
    Collections.sort(list);
    return list;
  }

  public static List<String> getSorted(Set<String> p) {
    List<String> list = new ArrayList<String>(p.size());
    for (Iterator<String> i = p.iterator(); i.hasNext();) {
      list.add(i.next());
    }
    Collections.sort(list);
    return list;
  }

  public static List<Integer> getSortedI(Set<Integer> p) {
    List<Integer> list = new ArrayList<Integer>(p.size());
    for (Iterator<Integer> i = p.iterator(); i.hasNext();) {
      list.add(i.next());
    }
    Collections.sort(list);
    return list;
  }

  public static List<String> getSortedKeys(Map<?, ?> p) {
    List<String> list = new ArrayList<String>(p.size());
    for (Object key : p.keySet()) {
      list.add("" + key);
    }
    Collections.sort(list);
    return list;
  }

  public static int[] toIntsArray(String str) {
    return toIntsArray(str, ",");
  }

  public static int[] toIntsArray(String str, String sep) {
    StringTokenizer t = new StringTokenizer(str, sep);
    List<Integer> l = new ArrayList<Integer>();
    while (t.hasMoreElements())
      l.add(Integer.parseInt(t.nextToken()));
    return toInts(l.toArray(new Integer[l.size()]));
  }

  public static int[] toInts(Integer[] ints) {
    int ii[] = new int[ints.length];
    for (int j = 0; j < ii.length; j++) {
      ii[j] = ints[j];
    }
    return ii;
  }

  public static String[] toStringArray(String str) {
    return toStringArray(str, ",");
  }

  public static String[] toStringArray(String str, String sep) {
    if (str == null)
      return new String[0];
    StringTokenizer t = new StringTokenizer(str, sep);
    List<String> l = new ArrayList<String>();
    while (t.hasMoreElements())
      l.add(t.nextToken());
    return l.toArray(new String[l.size()]);
  }

  public static List<String> find(String dir, String name, boolean returnAbs) {
    return find(new File(dir), name, returnAbs);
  }

  public static List<String> find(File dir, String name, boolean returnAbs) {
    GlobFilter filter = new GlobFilter(name);
    List<String> res = new ArrayList<String>();
    find(dir, "", filter, res, returnAbs);
    return res;
  }

  public static void find(File baseDir, String path, FileFilter filter, List<String> res,
      boolean returnAbs) {
    File dir = new File(baseDir, path);
    String[] list = dir.list();

    if (list == null)
      return;

    for (String f : list) {
      File ff = new File(dir, f);
      if (ff.isDirectory()) {
        find(baseDir, path + File.separator + f, filter, res, returnAbs);
      } else {
        if (filter.accept(ff)) {
          if (returnAbs) {
            res.add(baseDir + File.separator + path + File.separator + f);
          } else {
            res.add(path + File.separator + f);
          }
        }
      }
    }
  }

  public static byte[] readBytes(File ff) throws IOException {
    byte b[] = new byte[(int) ff.length()];
    DataInputStream din = new DataInputStream(new FileInputStream(ff));
    try {
      din.readFully(b);
    } finally {
      din.close();
    }
    return b;
  }

  public static String readAll(InputStream in) throws IOException {
    StringBuffer b = new StringBuffer();
    int c;
    while ((c = in.read()) != -1) {
      b.append((char) c);
    }
    return b.toString();
  }

  public static void cropImage(URL url, float x, float y, float w, float h, OutputStream out)
      throws IOException {
    InputStream in = url.openStream();
    try {
      cropImage(in, x, y, w, h, out);
    } finally {
      in.close();
    }
  }

  public static void cropImage(String imageName, float x, float y, float w, float h,
      OutputStream out) throws IOException {
    FileInputStream fin = new FileInputStream(imageName);
    try {
      cropImage(fin, x, y, w, h, out);
    } finally {
      fin.close();
    }

  }

  public static void cropImage(InputStream in, float x, float y, float w, float h, OutputStream out)
      throws IOException {
    if (x < 0)
      x = 0;
    if (y < 0)
      y = 0;
    if (x + w > 100)
      w = 100 - x;
    if (y + h > 100)
      h = 100 - y;

    BufferedImage bi = ImageIO.read(in);
    int width = bi.getWidth();
    int height = bi.getHeight();
    BufferedImage cropped = bi.getSubimage((int) (width * x / 100), (int) (height * y / 100),
        (int) (width * w / 100), (int) (height * h / 100));
    ImageIO.write(cropped, "jpeg", out);
  }

  public static List<Long> explodeLongs(String line, String sep) {
    List<Long> res = new ArrayList<Long>();
    StringTokenizer t = new StringTokenizer(line, sep);
    while (t.hasMoreElements()) {
      res.add(Long.parseLong(t.nextToken()));
    }
    return res;
  }

  public static List<Long> mergeLong(List<Long> l1, List<Long> l2, boolean sort) {
    Set<Long> set = new HashSet<Long>(l1);
    set.addAll(l2);
    List<Long> res = new ArrayList<Long>(set);
    if (sort) {
      Collections.sort(res);
    }
    return res;
  }

  public static boolean deltree(String dir) {
    File f = new File(dir);
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      for (File ff : files)
        deltree(ff.getAbsolutePath());
    }

    return f.delete();
  }

  public static boolean deltree(File root, String dir) {
    boolean b = deltree(new File(root, dir).getAbsolutePath());
    while (true) {
      if (dir != null && !dir.equals("") && !dir.equals(File.separator) && dir.length() > 0) {
        File f = new File(root, dir);
        if (f.isDirectory() && f.exists()) {
          File[] files = f.listFiles();
          if (files.length == 0) // empty dir.
            f.delete();
          else
            break;
        }

        dir = new File(dir).getParent();
      } else {
        break;
      }
    }
    return b;
  }

  public static String longsToString(Collection<Long> list) {
    return toString(list.toArray(new Long[list.size()]));
  }

  public static String intsToString(Collection<Integer> list) {
    return toString(list.toArray(new Integer[list.size()]));
  }

  public static String toString(Collection<String> list) {
    return toString(list.toArray(new String[list.size()]));
  }

  public static String toString(Object array[]) {
    return toString(array, ",");
  }

  public static String toString(int array[]) {
    return toString(array, ",");
  }

  public static String toString(long array[]) {
    return toString(array, ",");
  }

  public static String toString(double[] array) {
    return toString(array, ",");
  }

  public static String toString(Object array[], String sep) {
    return toString(array, sep, 0, array.length);
  }

  public static String toString(Object array[], String sep, int start, int end) {
    StringBuffer sb = new StringBuffer();
    for (int i = start; i < end; i++) {
      if (i > start)
        sb.append(sep);
      sb.append(array[i]);
    }
    return sb.toString();
  }

  public static String toString(long array[], String sep) {
    return toString(array, sep, 0, array.length);
  }

  public static String toString(double array[], String sep) {
    return toString(array, sep, 0, array.length);
  }

  public static String toString(int array[], String sep) {
    return toString(array, sep, 0, array.length);
  }

  public static String toString(double array[], String sep, int start, int end) {
    StringBuffer sb = new StringBuffer();
    for (int i = start; i < end; i++) {
      if (i > 0)
        sb.append(sep);
      sb.append(array[i]);
    }
    return sb.toString();
  }

  public static String toString(long array[], String sep, int start, int end) {
    StringBuffer sb = new StringBuffer();
    for (int i = start; i < end; i++) {
      if (i > 0)
        sb.append(sep);
      sb.append(array[i]);
    }
    return sb.toString();
  }

  public static String toString(int array[], String sep, int start, int end) {
    StringBuffer sb = new StringBuffer();
    for (int i = start; i < end; i++) {
      if (i > 0)
        sb.append(sep);
      sb.append(array[i]);
    }
    return sb.toString();
  }

  public static Properties loadProperties(String conf) throws IOException {
    Properties macros = new Properties();
    macros.setProperty("%BASE%", new File(conf).getAbsoluteFile().getParent());

    Properties p = new Properties();
    FileInputStream fin = new FileInputStream(conf);
    try {
      p.load(fin);
      for (Object o1 : p.keySet()) {
        String k1 = (String) o1;
        String v1 = p.getProperty(k1);
        Enumeration<Object> keys = macros.keys();
        while (keys.hasMoreElements()) {
          String k2 = (String) keys.nextElement();
          String v2 = macros.getProperty(k2);
          v1 = v1.replaceAll(k2, v2);
        }
        p.put(k1, v1);
      }
    } finally {
      try {
        fin.close();
      } catch (IOException e) {
      }
    }
    return p;
  }

  static SimpleDateFormat s_df;

  public static synchronized String formatDate(long ms) {
    return formatDate(new Date(ms));
  }

  public static String formatSize(long size) {
    return formatSize(size, false);
  }

    public static String formatSize(long size, boolean shortFormat) {
        StringBuffer sb = new StringBuffer();
        Formatter f = null;
        try {
        	f = new Formatter(sb);
        	double k = 1024;
        	double m = k * 1024;
        	double g = m * 1024;
        	long abs = Math.abs(size);
        	if (abs < k) {
        		f.format("%d", size);
        		if (shortFormat)
        			f.format("B");
        		else
        			f.format(" Bytes");
        	} else if (abs < m) {
        		f.format("%.1f", size / k);
        		if (shortFormat)
        			f.format("KB");
        		else
        			f.format(" KBytes");
        	} else if (abs < g) {
        		f.format("%.2f", size / m);
        		if (shortFormat)
        			f.format("MB");
        		else
        			f.format(" MBytes");
        	} else {
        		f.format("%.2f", size / g);
        		if (shortFormat)
        			f.format("GB");
        		else
        			f.format(" GBytes");
        	}
        } finally {
        	if (f != null) {
        		f.close();
        	}
        }
        return sb.toString();
    }

    /**
   * num [b|k|m|g], for example 1.2m
   *
   * @param s
   * @return number of bytes this string represents
   */
  public static long parseNumSize(String s) {
    long k = 0;
    if (s.length() == 1)
      return Integer.parseInt(s);

    char u = s.charAt(s.length() - 1);
    if (isLong("" + u))
      return Long.parseLong(s);

    float f = Float.parseFloat(s.substring(0, s.length() - 1));
    switch (u) {
    case 'b':
    k = (long) (f);
      break;
    case 'k':
    k = (long) (f * 1000);
      break;
    case 'm':
    k = (long) (f * 1000 * 1000);
      break;
    case 'g':
    k = (long) (f * 1000 * 1000 * 1000);
      break;
    default:
    throw new RuntimeException("Unsupported unit type " + u
        + ", supported units are b|k|m|g for byte|kilobyte|megabyte|gigabyte");
    }

    return k;
  }

    public static String formatNum(double size) {
        StringBuffer sb = new StringBuffer();
        Formatter f = null;
        try {
        	f = new Formatter(sb);
        	double k = 1000;
        	double m = k * 1000;
        	double g = m * 1000;
        	double abs = Math.abs(size);
        	if (abs < k) {
        		f.format("%.1f", size);
        	} else if (abs < m) {
        		f.format("%.1fK", (float) size / k);
        	} else if (abs < g) {
        		f.format("%.2fM", (float) size / m);
        	} else {
        		f.format("%.2fG", (float) size / g);
        	}
        } finally {
        	if (f != null) {
        		f.close();
        	}
        }

        return sb.toString();
    }

  public static synchronized String formatDate(Date d) {
    if (s_df == null)
      s_df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");

    return s_df.format(d);
  }

  public static String formatMilliSeconds(long ms) {
    return formatSeconds(ms / 1000);
  }

  public static String formatSeconds(long seconds) {
    long minutes = seconds / 60;
    seconds = seconds - minutes * 60;
    long hours = minutes / 60;
    minutes = minutes - hours * 60;
    String r = "";
    if (hours > 0)
      r += lpad("" + hours, '0', 2) + ":";
    if (hours > 0 || minutes > 0)
      r += lpad("" + minutes, '0', 2) + ":";
    return r + lpad("" + seconds, '0', 2);
  }

  public static String formatTimeLength(long ms) {
    int SECOND = 1000;
    int MINUTE = SECOND * 60;
    int HOUR = MINUTE * 60;
    int DAY = HOUR * 24;
    if (ms < SECOND)
      return ms + " ms";
    if (ms < MINUTE)
      return round((ms / (float) SECOND), 2) + " secs";
    if (ms < HOUR)
      return round((ms / (float) MINUTE), 2) + " mins";
    if (ms < DAY)
      return round((ms / (float) HOUR), 2) + " hours";
    else
      return round((ms / (float) DAY), 2) + " days";

  }

  private static String round(float f, int n) {
    int d = (int) Math.pow(10, n);
    return "" + ((int) (f * d)) / (float) d;
  }

  public static String lpad(String s, char c, int x) {
    StringBuffer b = new StringBuffer(x);
    int ps = x - s.length();
    while (ps-- > 0) {
      b.append(c);
    }
    b.append(s);
    return b.toString();
  }

  /**
   * Zip a file or a directory. Create a zip from the file represented by
   * <code>srcFile</code>, or all the files found under it (if a directory).
   * Output is saved to the file represented by <code>destFile</code>. Zip is
   * created compressed.
   *
   * @param srcFile File or directory to zip.
   * @param destFile Destination for the zipped output.
   * @throws IOException If IO exception occurs (messing with files here...)
   */
  public static void zip(File srcFile, File destFile, String archiveRoot) throws IOException {
    FileOutputStream fout = new FileOutputStream(destFile);
    try {
      zip(srcFile, fout, archiveRoot);
    } finally {
      fout.close();
    }
  }

  public static void zip(File srcFile, OutputStream out, String archiveRoot) throws IOException {
    Vector<File> filesVector = getFilesList(srcFile);
    if (filesVector.size() > 0) {
      String baseDirPath = null;
      if (srcFile.isDirectory()) {
        baseDirPath = srcFile.getPath();
      } else {
        baseDirPath = srcFile.getParent();
      }

      ZipOutputStream zipOut = new ZipOutputStream(out);
      try {
        File[] files = new File[filesVector.size()];
        ZipEntry[] entries = new ZipEntry[filesVector.size()];

        for (int i = 0; i < files.length; i++) {
          files[i] = filesVector.elementAt(i);
          String entryPath = archiveRoot + File.separatorChar
              + files[i].getPath().substring(baseDirPath.length() + 1);
          entries[i] = new ZipEntry(entryPath);
          zipOut.putNextEntry(entries[i]);
          entries[i].setTime(files[i].lastModified());
          if (!files[i].isDirectory()) {
            FileInputStream fileIn = new FileInputStream(files[i]);
            try {
              pump(fileIn, zipOut);
            } finally {
              fileIn.close();
            }
          }
          zipOut.closeEntry();
        }
      } finally {
        zipOut.finish();
        zipOut.close();
      }
    }
  }

  /**
   * Lists all the files in a given directory (recursively).
   *
   * @param baseDir Base directory for which list is created
   * @return Vector of File objects representing all the files in baseDir.
   */
  private static Vector<File> getFilesList(File baseDir) {
    Vector<File> filesVector = new Vector<File>();
    String[] filesList = null;

    if (baseDir.isDirectory()) {
      filesList = baseDir.list();
    } else {
      filesList = new String[] { "" };
    }

    for (int i = 0; i < filesList.length; i++) {
      File file = new File(baseDir, filesList[i]);
      if (file.isDirectory()) {
        Vector<File> innerFilesVector = getFilesList(file);
        Enumeration<File> enum1 = innerFilesVector.elements();
        while (enum1.hasMoreElements()) {
          filesVector.addElement(enum1.nextElement());
        }
      } else {
        filesVector.addElement(file);
      }
    }

    return filesVector;
  }

  /**
   * Writes the bytes read from the given input stream into the given output
   * stream until the end of the input stream is reached. Returns the amount of
   * bytes actually read/written.
   */
  public static int pump(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[4096];
    int count;
    int amountRead = 0;

    while ((count = in.read(buf)) != -1) {
      out.write(buf, 0, count);
      amountRead += count;
    }

    return amountRead;
  }

  public static long toLong(String s) {
    return toLong(s, -1);
  }

  public static long toLong(String s, long def) {
    return s == null || s.length() == 0 || s.equals("null") ? def : Long.parseLong(s);
  }

  public static float toFloat(String s) {
    return toFloat(s, -1);
  }

  public static float toFloat(String s, float def) {
    return (s == null || s.length() == 0 || s.equals("null")) ? def : Float.parseFloat(s);
  }

  public static int toInt(String s) {
    return toInt(s, -1);
  }

  public static int toInt(String s, int def) {
    return (s == null || s.length() == 0 || s.equals("null")) ? def : Integer.parseInt(s);
  }

  public static boolean toBoolean(String s, boolean def) {
    return (s == null || s.length() == 0 || s.equals("null")) ? def : Boolean.valueOf(s);
  }

  public static int compare(float f1, float f2) {
    return f1 > f2 ? 1 : f1 < f2 ? -1 : 0;
  }

  public static int compare(long f1, long f2) {
    return f1 > f2 ? 1 : f1 < f2 ? -1 : 0;
  }

  public static void copyFile(File src, File target) throws IOException {
    BufferedInputStream in = null;
    BufferedOutputStream out = null;
    try {
      in = new BufferedInputStream(new FileInputStream(src));
      out = new BufferedOutputStream(new FileOutputStream(target));
      pump(in, out);
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
        }
      if (out != null)
        try {
          out.close();
        } catch (IOException e) {
        }
    }
  }

  public static String replaceAll(String source, String toReplace, String replacement) {
    int idx = source.lastIndexOf(toReplace);
    if (idx != -1) {
      StringBuffer ret = new StringBuffer(source);
      ret.replace(idx, idx + toReplace.length(), replacement);
      while ((idx = source.lastIndexOf(toReplace, idx - 1)) != -1) {
        ret.replace(idx, idx + toReplace.length(), replacement);
      }
      source = ret.toString();
    }

    return source;
  }

  public static String[] asStringArr(Collection<?> collection) {
    String res[] = new String[collection.size()];
    int i = 0;
    for (Object s : collection) {
      res[i++] = String.valueOf(s);
    }
    return res;
  }

  public static void readFully(InputStream in, byte[] data) throws IOException {
    int len = data.length;
    int off = 0;

    int n = 0;
    while (n < len) {
      int count = in.read(data, off + n, len - n);
      if (count < 0)
        throw new EOFException();
      n += count;
    }
  }

  public static void saveData(byte[] data, File file) throws IOException {
    FileOutputStream fout = new FileOutputStream(file);
    try {
      fout.write(data);
    } finally {
      fout.close();
    }
  }

  public static String[] toStringArray(int[] iii) {
    String s[] = new String[iii.length];
    int j = 0;
    for (int i : iii) {
      s[j++] = "" + i;
    }

    return s;
  }

  /**
   * @param s time specification, number[s|m|h|d] for example, 10 : 10 seconds.
   *          10s : 10 seconds. 12m : 12 minutes. 2h : 2 hours 5d : 5 days.
   * @return the number of seconds in the specified string (10m = 60 seconds)
   */
  public static int getSeconds(String s) {
    int k;
    if (s.length() == 1)
      return Integer.parseInt(s);

    char u = s.charAt(s.length() - 1);
    if (isInteger("" + u))
      return Integer.parseInt(s);

    float f = Float.parseFloat(s.substring(0, s.length() - 1));
    switch (u) {
    case 's':
    k = (int) (f);
      break;
    case 'm':
    k = (int) (f * 60);
      break;
    case 'h':
    k = (int) (f * 60 * 60);
      break;
    case 'd':
    k = (int) (f * 60 * 60 * 24);
      break;
    default:
    throw new RuntimeException("Unsupported unit type " + u
        + ", supported units are s : seconds | m : minutes | h : hours | d : days");
    }

    return k;
  }

  public static void saveLongList(String file, Collection<Long> c, boolean append)
      throws FileNotFoundException, IOException {
    File p = new File(file).getParentFile();
    if (p != null)
      p.mkdirs();

    FileOutputStream fout = new FileOutputStream(file, append);
    try {
      for (Long subjectID : c) {
        fout.write((subjectID + "\n").getBytes());
      }
    } finally {
      fout.close();
    }
  }

  public static void saveStringsList(String file, Collection<String> c)
      throws FileNotFoundException, IOException {
    saveStringsList(file, c, false);
  }

  public static void saveStringsList(String file, Collection<String> c, boolean append)
      throws FileNotFoundException, IOException {
    File p = new File(file).getParentFile();
    if (p != null)
      p.mkdirs();

    FileOutputStream fout = new FileOutputStream(file, append);
    try {
      for (String subjectID : c) {
        fout.write((subjectID + "\n").getBytes());
      }
    } finally {
      fout.close();
    }
  }

  public static void saveLongList(String file, Collection<Long> c) throws FileNotFoundException,
      IOException {
    saveLongList(file, c, false);
  }

  public static String[] toStringArray(Collection<?> keysCollection) {
    String[] keys = new String[keysCollection.size()];

    int i = 0;
    for (Object obj : keysCollection) {
      keys[i++] = obj.toString();
    }

    return keys;
  }

  public static <T> List<T> getChunk(List<T> list, int n, int chunkMaxSize) {
    int a = n * chunkMaxSize;
    int b = Math.min(list.size(), (n + 1) * chunkMaxSize);
    List<T> chunk = new ArrayList<T>(b - a);
    for (int i = a; i < b; i++) {
      chunk.add(list.get(i));
    }
    return chunk;
  }

  public static <T> List<List<T>> getChunks(List<T> list, int chunkMaxSize) {
    if (chunkMaxSize <= 0)
      throw new IllegalArgumentException("Non positive chunk max size " + chunkMaxSize);
    int numChunks = (int) Math.ceil(list.size() / (float) chunkMaxSize);
    List<List<T>> chunks = new ArrayList<List<T>>(numChunks);
    for (int i = 0; i < numChunks; i++) {
      chunks.add(getChunk(list, i, chunkMaxSize));
    }
    return chunks;
  }

  public static Set<Long> parseLongs(String longs) {
    HashSet<Long> l = new HashSet<Long>();
    StringTokenizer t = new StringTokenizer(longs, ",");
    while (t.hasMoreTokens()) {
      l.add(Long.parseLong(t.nextToken()));
    }
    return l;
  }

  public static Set<String> parseStrings(String s) {
    HashSet<String> l = new HashSet<String>();
    StringTokenizer t = new StringTokenizer(s, ",");
    while (t.hasMoreTokens()) {
      l.add(t.nextToken());
    }
    return l;
  }

  public static char[] getSlashedPath(String s) {
    StringBuilder b = new StringBuilder(s.length() * 2);
    for (int i = 0; i < s.length(); i++) {
      b.append(s.charAt(i)).append('/');
    }
    return b.toString().toCharArray();
  }

  public static char[] getSlashedPath(long num) {
    long n = num;
    if (n < 0)
      throw new RuntimeException("negative id " + n);
    int len = 0;
    if (n == 0)
      len = 1;
    else
      len = (int) (Math.log10(n) + 1);
    int blen = len * 2;
    char res[] = new char[blen];
    int i = blen - 1;
    while (true) {
      long d = n / 10;
      long n1 = d * 10;
      long n2 = n - n1;
      char c = (char) (n2 + '0');
      res[i--] = File.separatorChar;
      res[i--] = c;
      n = d;
      if (n == 0)
        break;
    }
    return res;
  }

  public static int smartCompare(String s1, String s2) {
    int i1 = s1.lastIndexOf('-');
    int i2 = s2.lastIndexOf('-');
    if (i1 != -1 && i2 != -1) {
      String h1 = s1.substring(0, i1);
      String h2 = s2.substring(0, i2);
      int c = h1.compareTo(h2);
      if (c == 0) {
        String t1 = s1.substring(i1 + 1);
        String t2 = s2.substring(i2 + 1);
        if (isInteger(t1) && isInteger(t2)) {
          return Integer.parseInt(t1) - Integer.parseInt(t2);
        } else {
          return s1.compareTo(s2);
        }
      } else {
        return s1.compareTo(s2);
      }
    } else {
      return s1.compareTo(s2);
    }
  }

  public static Collection<Integer> now(Collection<Future<Integer>> s) throws InterruptedException,
      ExecutionException {
    Set<Integer> res = new HashSet<Integer>();
    for (Future<Integer> r : s)
      res.add(r.get());
    return res;
  }

  public static String addressToString(byte[] ip) {
    // Convert to dot representation
    String ipAddrStr = "";
    for (int i = 0; i < ip.length; i++) {
      if (i > 0) {
        ipAddrStr += ".";
      }
      ipAddrStr += ip[i] & 0xFF;
    }

    return ipAddrStr;
  }

  public static String getCurrentEnvironmentNetworkIp() throws SocketException {

    Enumeration<NetworkInterface> netInterfaces = null;

    netInterfaces = NetworkInterface.getNetworkInterfaces();

    while (netInterfaces.hasMoreElements()) {
      NetworkInterface ni = netInterfaces.nextElement();
      Enumeration<InetAddress> address = ni.getInetAddresses();
      while (address.hasMoreElements()) {
        InetAddress addr = address.nextElement();
        if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()
            && !(addr.getHostAddress().indexOf(":") > -1)) {
          return addr.getHostAddress();
        }
      }
    }
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "127.0.0.1";
    }
  }

  public static String ensureDir(String s) {
    if (s.endsWith("/") || s.endsWith("\\"))
      return s;
    else
      return s + File.separator;
  }

  public static String md5(String d) {
    return md5(d.getBytes());
  }

  public static String md5(byte[] b) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      m.reset();
      m.update(b);
      byte[] digest = m.digest();
      BigInteger bigInt = new BigInteger(1, digest);
      String hashtext = bigInt.toString(16);
      // Now we need to zero pad it if you actually want the full 32
      // chars.
      while (hashtext.length() < 32) {
        hashtext = "0" + hashtext;
      }
      return hashtext;

    } catch (NoSuchAlgorithmException nsae) {
      return "ERROR";
    }

  }

  public static int stringHash(String s) {
    byte[] bytes;
    try {
      bytes = s.getBytes("utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 not supported?", e);
    }
    String d = md5(bytes).substring(0, 7).toUpperCase();
    return Integer.parseInt(d, 16);
  }

  public static int stringMod(String s, int mod) {
    return stringHash(s) % mod;
  }

  public static int stringMod(byte s[], int mod) {
    String d = md5(s).substring(0, 7).toUpperCase();
    int ii = Integer.parseInt(d, 16);
    return ii % mod;
  }

  public static boolean isUrl(String u) {
    try {
      new URL(u);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  /**
   * num [b|k|m|g], for example 1.2m
   *
   * @param s
   * @return number of bytes this string represents
   */
  public static long parseByteSize(String s) {
    long k = 0;
    if (s.length() == 1)
      return Integer.parseInt(s);

    char u = s.charAt(s.length() - 1);
    if (isLong("" + u))
      return Long.parseLong(s);

    float f = Float.parseFloat(s.substring(0, s.length() - 1));
    switch (u) {
    case 'b':
    k = (long) (f);
      break;
    case 'k':
    k = (long) (f * 1024);
      break;
    case 'm':
    k = (long) (f * 1024 * 1024);
      break;
    case 'g':
    k = (long) (f * 1024 * 1024 * 1024);
      break;
    default:
    throw new RuntimeException("Unsupported unit type " + u
        + ", supported units are b|k|m|g for byte|kilobyte|megabyte|gigabyte");
    }

    return k;
  }

  public static Map<String, List<String>> getUrlParameters(String url)
      throws UnsupportedEncodingException {
    Map<String, List<String>> params = new HashMap<String, List<String>>();
    String[] urlParts = url.split("\\?");
    if (urlParts.length > 1) {
      String query = urlParts[1];
      for (String param : query.split("&")) {
        String pair[] = param.split("=");
        String key = URLDecoder.decode(pair[0], "UTF-8");
        String value = "";
        if (pair.length > 1) {
          value = URLDecoder.decode(pair[1], "UTF-8");
        }
        List<String> values = params.get(key);
        if (values == null) {
          values = new ArrayList<String>();
          params.put(key, values);
        }
        values.add(value);
      }
    }
    return params;
  }
}
