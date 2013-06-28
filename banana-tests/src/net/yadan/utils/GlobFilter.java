/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * @author omry 
 * @date Dec 18, 2008
 */
public class GlobFilter implements FileFilter, FilenameFilter
{
	Pattern pattern;

	String globPattern;

	private boolean m_invert;
	
	boolean m_includeDirs = true;

	public GlobFilter(String pattern)
	{
		this(pattern, false);
	}
	
	public GlobFilter(String pattern, boolean invert)
	{
		setPattern(pattern);
		m_invert = invert;
	}
	
	public GlobFilter(String pattern, boolean invert, boolean includeDirs)
	{
		setPattern(pattern);
		m_invert = invert;
		m_includeDirs = includeDirs;
	}

	public void setPattern(String globPattern)
	{
		this.globPattern = globPattern;
		this.pattern = globPattern(globPattern);
	}

	public static Pattern globPattern(String globPattern)
	{
		char[] gPat = globPattern.toCharArray();
		char[] rPat = new char[gPat.length * 2];
		boolean isWin32 = (File.separatorChar == '\\');
		boolean inBrackets = false;
		int j = 0;


		if (isWin32)
		{
			// On windows, a pattern ending with *.* is equal to ending with
			// *
			int len = gPat.length;
			if (globPattern.endsWith("*.*"))
			{
				len -= 2;
			}
			for (int i = 0; i < len; i++)
			{
				switch (gPat[i])
				{
				case '*':
					rPat[j++] = '.';
					rPat[j++] = '*';
					break;

				case '?':
					rPat[j++] = '.';
					break;

				case '\\':
					rPat[j++] = '\\';
					rPat[j++] = '\\';
					break;

				default:
					if ("+()^$.{}[]".indexOf(gPat[i]) >= 0)
					{
						rPat[j++] = '\\';
					}
					rPat[j++] = gPat[i];
					break;
				}
			}
		}
		else
		{
			for (int i = 0; i < gPat.length; i++)
			{
				switch (gPat[i])
				{
				case '*':
					if (!inBrackets)
					{
						rPat[j++] = '.';
					}
					rPat[j++] = '*';
					break;

				case '?':
					rPat[j++] = inBrackets ? '?' : '.';
					break;

				case '[':
					inBrackets = true;
					rPat[j++] = gPat[i];

					if (i < gPat.length - 1)
					{
						switch (gPat[i + 1])
						{
						case '!':
						case '^':
							rPat[j++] = '^';
							i++;
							break;

						case ']':
							rPat[j++] = gPat[++i];
							break;
						}
					}
					break;

				case ']':
					rPat[j++] = gPat[i];
					inBrackets = false;
					break;

				case '\\':
					if (i == 0 && gPat.length > 1 && gPat[1] == '~')
					{
						rPat[j++] = gPat[++i];
					}
					else
					{
						rPat[j++] = '\\';
						if (i < gPat.length - 1 && "*?[]".indexOf(gPat[i + 1]) >= 0)
						{
							rPat[j++] = gPat[++i];
						}
						else
						{
							rPat[j++] = '\\';
						}
					}
					break;

				default:
					// if ("+()|^$.{}<>".indexOf(gPat[i]) >= 0) {
					if (!Character.isLetterOrDigit(gPat[i]))
					{
						rPat[j++] = '\\';
					}
					rPat[j++] = gPat[i];
					break;
				}
			}
		}
		Pattern pat = Pattern.compile(new String(rPat, 0, j), Pattern.CASE_INSENSITIVE);
		return pat;
	}

	public boolean accept(File f)
	{
		return accept(f.getParentFile(), f.getName());
	}

	public boolean matches(String name)
	{
		boolean b = pattern.matcher(name).matches();
		if (m_invert) return !b;
		else return b;
	}

	@Override
	public String toString()
	{
		return "Filter: " + globPattern;
	}

	@Override
	public boolean accept(File dir, String name)
	{
		File f = new File(dir, name);
		if (m_includeDirs && f.isDirectory())
		{
			return true;
		}
		return matches(name);
	}
}
