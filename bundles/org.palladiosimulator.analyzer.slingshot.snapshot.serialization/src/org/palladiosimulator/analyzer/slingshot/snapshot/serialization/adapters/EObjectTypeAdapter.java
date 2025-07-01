package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.exception.ModelElementWriteException;
import org.palladiosimulator.commons.emfutils.EMFLoadHelper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.uka.ipd.sdq.identifier.Identifier;

public class EObjectTypeAdapter extends TypeAdapter<EObject> {

	private final ResourceSet set;
	
	private final Map<String, URI> URIMap;
	
	public EObjectTypeAdapter(final ResourceSet set) {
		super();
		this.set = set;

		URIMap = new HashMap<>();
		for (final Resource resource : set.getResources()) {
			
			final URI uri = resource.getURI();
			final String filename = uri.lastSegment();
			
			URIMap.put(filename, uri);
		}
	}

	/**
	 * Write the string representation of the given model element to Json. 
	 * 
	 * If the model element is not contained in a resource, no string representation can be created and an exception is thrown. 
	 * 
	 * If the model element is contained in a resource with a file URI, only the file name and the fragment are written to json.
	 * If the model element is contained in a resource that has no file URI, e.g. because it the resource has a pathmap-URI, the erntire URI is written to json.
	 * 
	 * @throws IOException if the model element is no contained in a resource. 
	 */
	@Override
	public void write(final JsonWriter out, final EObject value) throws IOException {	
		
		if (value.eResource() == null) {
			throw new ModelElementWriteException(
					String.format("Cannot create String representation for %s[%s] because resource is null.",
							value.toString(), Identifier.class.isInstance(value) ? ((Identifier) value).getId() : ""));
		}
		
		final URI uri = EcoreUtil.getURI(value);
		if (uri.isFile()) {
			final String file = new File(uri.toFileString()).toPath().getFileName().toString();
			final String fragment = uri.fragment();

			final URI relativeUri = URI.createURI(file).appendFragment(fragment); 
			out.value(relativeUri.toString());
		} else {
			out.value(uri.toString());
		}
					
	}

	/**
	 * Reads the string representation of a model element to the actual model element. 
	 * 
	 * @throws IOException if string representing the model element does not contain a file name.
	 */
	@Override
	public EObject read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		
		final String file = s.split("#")[0];
		
		if (URIMap.containsKey(file)) {
			final URI uri = URIMap.get(file).appendFragment(s.split("#")[1]);
			return EMFLoadHelper.loadAndResolveEObject(set, uri); 
		}
		if (file == "") {
			throw new IOException(String.format("Cannot load model element with fragment %s from resource, because file name is missing in json.", s.split("#")[1]));
		}
		
		return EMFLoadHelper.loadAndResolveEObject(set, s); 
		
	}
}
