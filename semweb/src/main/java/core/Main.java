package core;

import lombok.val;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.PropertyConfigurator;

public class Main {
	
	private Main() {}
	
	private static String shortenURI(String uri) {
		val tokens = uri.split("\\/");
		val maxIndex = tokens.length - 1;

		return tokens[maxIndex]; 
	}
	
	public static void extractFromInstance(String filename) {
		PropertiesConfiguration config;
		try {
			config = new PropertiesConfiguration(filename);

			val endpoint = config.getString("endpoint");
			val instance = config.getString("instance");
			val minDepth = config.getInt("mindepth");
			val maxDepth = config.getInt("maxdepth");
			val outdir = config.getString("outdir");
			
			for (int i = minDepth; i <= maxDepth; i++) {
				extractFromInstance(endpoint, instance, i, outdir);
			}

		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public static void extractFromInstance(
			String endpoint, String instance, int depth, String outdir) {

		val fragment = Fragment.from(endpoint);
		fragment.expand( instance, depth );
		val outfile = outdir + shortenURI(instance) + "_dep" + depth + ".ttl";
		fragment.output( outfile );
	}
	
	public static void main(String[] args) {
		if ( args.length == 2 ) {
			PropertyConfigurator.configure(args[1]);
			extractFromInstance(args[0]);
		}

		extractFromInstance("crawler.properties");
	}
	
}
