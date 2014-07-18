package core;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

@Slf4j
public class Main {
	
	private Main() {}
	
	private static String shortenURI(String uri) {
		val tokens = uri.split("\\/");
		val maxIndex = tokens.length - 1;

		return tokens[maxIndex]; 
	}
	
	public static void main(String[] args) {
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
	
}
