package lemmatizer;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import trie.Trie;
import util.Options;

/**
 * <p>
 * Keep a distribution of (inflectional) classes for each node in a suffix / prefix trie.
 * Return suffix-dependent probabilities according to the TnT model.
 * <p>
 * Other 'unknown word models' suggested as superior  in the literature (word shape based classifiers)
 * will be considered. 
 * <p>
 * @author taalbank
 *
 */
public class SuffixGuesser implements Classifier
{
	trie.Trie suffixTrie = new Trie();
	trie.Trie prefixTrie = new Trie();
	boolean useVarianceForSmoothing = false;
	Set<String> allClasses = new HashSet<String>();

	/**
	 * The maximum suffix length taken into account.
	 * Should be at least equal to the longest suffix or prefix found while extracting patterns!!
	 */

	int M = 10;
	/**
	 * The smoothing parameters.
	 */
	double[] theta  = new double[M+1];
	Distribution zeroDistribution = null;

	/**
	 * PM: voeg woordstartmarkeerder toe om, enz?
	 * @param w
	 * @param cls
	 */
	void addWordToSuffixTrie(String w, String cls)
	{
		allClasses.add(cls);
		if (w.length() < M)
		{
			w = "^" + w;
		}
		String wRev = new StringBuffer(w).reverse().toString();
		suffixTrie.root.putWord(wRev);
		int n = w.length();
		Trie.TrieNode node = suffixTrie.root;
		int i=0;
		for (i=0; i <= M && i < n; i++)
		{
			Distribution d;  	
			if (node.data == null) node.data = (d = new Distribution());
			else d = (Distribution) (node.data);

			d.incrementCount(cls);


			Trie.TrieNode nextNode = node.delta(w.charAt(n-1-i));
			if (nextNode == null) break; else 	node = nextNode;
		}
		// System.err.println(w + ":  " + cls + " inserted: " +  i);
	}

	/**
	 * Before the collected statistics can be used, we need to:
	 * <ul>
	 * <li> Compute the observed class distributions per node;
	 * <li>Set theta values for smoothing
	 * </ul>
	 */

	public void complete()
	{
		computeSuffixDistributions();
		computeThetas();
	}

	public void computeSuffixDistributions()
	{
		Trie.NodeAction action = new Trie.NodeAction()
		{
			//public int N=0;
			public void doIt(Trie.TrieNode n)
			{ 
				//N++;

				Distribution d = (Distribution) n.data;
				if (d != null)
				{
					d.computeProbabilities();
				} else
				{
					// System.err.println("no distribution defined at node: " + n);
				}
			}
		};
		suffixTrie.forAllNodesPreOrder(action);
		//System.err.println("trie has " + action.N + " nodes");
	}

	//@Override
	public String classifyItem(Item i)
	{
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public Distribution distributionForItem(Item i)
	{
		String s = i.values.get(0);
		return distributionForString(s);
	}

	public Distribution observedDistributionAtNode(Trie.TrieNode n)
	{
		return (Distribution) n.data;
	}

	public void computeThetas()
	{
		for (int i=0; i <=  M; i++)
			theta[i] = 0.3; // arbitrary smoothing choice

		theta[0] = 0.05;  theta[1] = 0.1; theta[3] = 0.2;

		if (!useVarianceForSmoothing)  return;

		Distribution d0 = observedDistributionAtNode(suffixTrie.root);
		if (d0.size() <= 1)
			return;

		/**
		 * Compute theta according to Brants' variance rule. Not smooth enough
		 * ToDo: ... better smoothing principles
		 */

		double s = d0.size();
		double Pavg =0;
		for (Distribution.Item i: d0.items) Pavg += i.p; Pavg /= s;

				double theta0 = 0;
				for (Distribution.Item i: d0.items) theta0 += (i.p - Pavg) *  (i.p - Pavg); theta0  /= s-1;

						System.err.println("theta0 =  " + theta0);
						//System.exit(0);
						for (int i=0; i <=  M; i++)
							theta[i] = theta0;
	}

	/**
	 * Calculates suffix-dependent class distribution
	 * <br/><br/>
	 * @param s
	 * @return
	 */
	public Distribution distributionForString(String s)
	{
		if (zeroDistribution == null)
			zeroDistribution = new Distribution(allClasses);
		zeroDistribution.resetToZero();

		// the following is rather slow: for better performance one should precompile the smoothed distribution
		if (s.length() < M)
		{
			s = "^" + s;
		}
		int n = s.length();

		Trie.TrieNode[]  path = new Trie.TrieNode[M+n];
		path[0] = this.suffixTrie.root;

		int i = n;
		for (i= 0;  i  < M && i < n ;  i++)
		{
			int c = s.charAt(n-1-i);
			path[i+1] = path[i].delta(c);
			if (path[i+1] == null) break;
		}
		//System.err.println(s + " match suffix length:  " + i);
		Distribution d = zeroDistribution;
		d.mergeHigherOrderDistribution(observedDistributionAtNode(path[0]), 0.0);
		for (int j=1; j  < M && path[j] != null;  j++)
		{
			Distribution dnext = observedDistributionAtNode(path[j]);
			if (dnext == null)
			{
				// System.err.println("?! No distribution defined for  " + s + " at " + j + " node = "  + path[j]);
			}
			else
				d.mergeHigherOrderDistribution(dnext, theta[j]);
			//System.err.println("s= " + s + " j= " + j + " d= " + d);
		}
		return d;
	}

	//@Override
	public void save(String filename) throws IOException
	{
		// TODO Auto-generated method stub

	}

	//@Override
	public void train(Dataset d)
	{
		d.features.finalize();
		for (Item  i: d.items)
		{
			String s = i.values.get(0); // Vieze truc;
			String cls = i.classLabel;
			addWordToSuffixTrie(s,cls);
		}
		complete();	
	}

	//@Override
	public void train(Dataset d, int MAX_ITEMS_USED)
	{
		d.reduceItems(MAX_ITEMS_USED);
		train(d);
	}

	public void setType(String t)
	{
		// no types implemented yet...
	}

	public static void main(String[] args)
	{
		new util.Options(args);
		FeatureSet fs = new FeatureSet.Dummy();
		ClassifierSet cs = new ClassifierSet(fs, "lemmatizer.SuffixGuesser");
		ReverseLemmatizer rl = new ReverseLemmatizer(new SimplePatternFinder(), cs);

		String referenceLexicon = Options.getOption("referenceLexicon");
		//ParadigmExpander pe = new PrefixSuffixGuesser();
		if (Options.getOption("command") != null && Options.getOption("command").equals("test"))
		{
			ReverseLemmatizationTest test = new ReverseLemmatizationTest(referenceLexicon);
			test.runTest(rl);
		} else
		{
			rl.findInflectionPatterns(Options.getOption("trainFile"));
			rl.expandLemmaList(Options.getOption("testFile"));
		}
	}
}
