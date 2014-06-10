package core;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
	
	private Main() {}
	
	private static String shortenURI(String uri) {
		val tokens = uri.split("\\/");
		val maxIndex = tokens.length - 1;

		return tokens[maxIndex]; 
	}
	
	public static void main(String[] args) {
		val fragment = EndPointFragment.fromDBPedia();
		val clazz = "http://dbpedia.org/ontology/SpaceShuttle";
		val depth = 3;
		val dir = "/home/tac/";
		
		val outputFile = dir + shortenURI(clazz) + "_dep" + depth + ".ttl";

		fragment.expand( clazz, depth );
		log.info( "finish crawling." );
		
		fragment.output( outputFile );
		log.info( "finish output to {}", outputFile );
	}
	
}
