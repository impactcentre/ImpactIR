package lexicon;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WordForm
{
	@XmlElement
	public String lemma = "";
	@XmlElement
	public String wordform = "";
	public String tag = "nil";
	@XmlElement
	public String lemmaPoS = "nil";
	
	public int wordformFrequency = 0;
	public int lemmaFrequency = 0;

	public String toString()
	{
               if (wordformFrequency > 0 || lemmaFrequency > 0)
               {
                   return "{" + lemma + "," + wordform + "," + tag + ", f(w)=" + wordformFrequency + ", f(l)=" + lemmaFrequency + "}";
               } else
		return "{" + lemma + "," + wordform + "," + tag + "}";
	}
	
	public String toStringTabSeparated()
	{
		return wordform + "\t" + lemma + "\t" + tag + "\t" + lemmaPoS;
	}
	
	public boolean equals(Object other)
	{
		try
		{
			WordForm wf = (WordForm) other;
			return lemma.equals(wf.lemma) && wordform.equals(wf.wordform)
					&& tag.equals(wf.tag) && lemmaPoS.equals(wf.lemmaPoS);
		} catch (Exception e)
		{
			return false;
		}
	}

	public int hashCode()
	{
		return lemma.hashCode() + wordform.hashCode() + tag.hashCode();
	}
}
