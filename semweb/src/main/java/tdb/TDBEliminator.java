package tdb;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;

public class TDBEliminator extends AbstractEliminator {
	
	private Dataset dataset;

	public TDBEliminator(Dataset dataset) {
		this.dataset = dataset;
	}

	@Override
	public GraphStore createGraphStore() {
		return GraphStoreFactory.create(dataset);
	}
	
	@Override
	public void updateBegin() {
		dataset.begin(ReadWrite.WRITE);	
	}
	
	@Override
	public void updateEnd() {
		dataset.commit();
		dataset.end();
	}
	
}
