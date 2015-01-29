package core;

import java.io.FileWriter;

import lombok.AllArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@Slf4j
@AllArgsConstructor
public class Fragment {

	private final Model   model;
	private final Crawler crawler;
	
	public static Fragment from(String endpointURL) {
		val model   = ModelFactory.createDefaultModel();
		val crawler = new Crawler(endpointURL);

		return new Fragment( model, crawler );
	}
	
	public void expand(String instance, int depth) {
		val i = ResourceFactory.createResource( instance );
		
		expand(i, depth);
	}
	
	private void expand(Resource instance, int depth) {
		if ( !crawler.exists(instance) ) {
			log.info("\"{}\" does not exist.", instance);
			log.info("aborted.");

			return ;
		}

		log.info("expand({}, {})", instance, depth);

		val stopwatch = Stopwatch.createStarted();
		
		model.add( crawler.tracePropertyPathFrom(instance, depth) );
		log.info("finish tracing property path");
		
		model.add( crawler.inferSuperPropertyIn(model) );
		log.info("finish inference for super property");
		
		model.add( crawler.extractPropertyInfo(model) );
		log.info("finish extracting property info");
		
		model.add( crawler.extractTypeIn(model));
		log.info("finish extracting type");
		
		model.add( crawler.inferSuperClassIn(model) );
		log.info("finish inference for super class");

		stopwatch.stop();
		log.info("{},{}: time = {}", instance, depth, stopwatch);
	}
	
	public void output(String fileName) {
		try ( FileWriter out = new FileWriter(fileName) ) {
			model.write( out, "TTL" );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
