package core;

import java.io.FileWriter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class EndPointFragment {

	private final Model model;
	private final Crawler crawler;
	
	public static EndPointFragment fromDBPedia() {
		val model   = ModelFactory.createDefaultModel();
		val crawler = Crawler.DBPEDIA;

		return new EndPointFragment( model, crawler );
	}
	
	public void expand(String clazz, int depth) {
		val c = ResourceFactory.createResource( clazz );
		
		expand(c, depth);
	}
	
	private void expand(Resource clazz, int depth) {
		model.add( crawler.extractClassInfo(clazz) );
		model.add( crawler.extractPropertyPath(model, depth) );
		model.add( crawler.extractPropertyInfo(model) );
	}
	
	public void output(String fileName) {
		FileWriter out = null;
		try {
			out = new FileWriter( fileName );
			model.write( out, "TTL" );
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
