package tdb;

import lombok.val;

import org.apache.jena.atlas.lib.StrUtils;

import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;

public abstract class AbstractEliminator implements Eliminator {

	public abstract GraphStore createGraphStore();
	
	public void deleteNonResource() {
		updateBegin();

		val delete    = StrUtils.strjoinNL(
				"delete { ?s ?p ?o . }",
				"where  { ?s ?p ?o . filter( !isURI(?s) || !isURI(?o) ) }" );
		val request   = UpdateFactory.create(delete);
		val store     = createGraphStore();
		val processor = UpdateExecutionFactory.create(request, store);
		processor.execute();
		
		updateEnd();
	}
	
	protected void updateBegin() {}
	
	protected void updateEnd()   {}

}
