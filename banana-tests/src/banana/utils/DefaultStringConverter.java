package banana.utils;

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

