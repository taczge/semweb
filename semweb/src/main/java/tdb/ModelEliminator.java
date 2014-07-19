package tdb;

import lombok.AllArgsConstructor;
import lombok.ToString;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;

@AllArgsConstructor
@ToString
public class ModelEliminator extends AbstractEliminator {
	
	private Model model;

	@Override
	public GraphStore createGraphStore() {
		return GraphStoreFactory.create(model);
	}

	@Override
	public int hashCode() {
		return model.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelEliminator other = (ModelEliminator) obj;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.isIsomorphicWith(other.model))
			return false;
		return true;
	}

}
