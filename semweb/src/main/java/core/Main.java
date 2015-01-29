package core;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.PropertyConfigurator;

@Slf4j
public class Main {
	
	private Main() {}
	
	private static String shortenURI(String uri) {
		val tokens = uri.split("\\/");
		val maxIndex = tokens.length - 1;

		return tokens[maxIndex]; 
	}
	
	public static void extractFromClass() {
		PropertiesConfiguration config;
		try {
			config = new PropertiesConfiguration("crawler.properties");

			val endpoint = config.getString("endpoint");
			val clazz = config.getString("class");
			val depth = config.getInt("depth");
			val outdir = config.getString("outdir");

			val outputFile = outdir + shortenURI(clazz) + "_dep" + depth + ".ttl";

			val fragment = EndPointFragment.from(endpoint);
			fragment.expand( clazz, depth );
			log.info( "finish crawling." );
			
			fragment.output( outputFile );
			log.info( "finish output to {}", outputFile );
			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
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
		// extractFromClass();
	}
	
}
