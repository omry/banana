/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.utils;

/**
 * @author omry 
 * @date Apr 30, 2009
 */
public class DefaultStringConverter implements StringConverter
{

	@Override
	public String toString(Object o)
	{
		return o != null ? o.toString() : "null";
	}

}

