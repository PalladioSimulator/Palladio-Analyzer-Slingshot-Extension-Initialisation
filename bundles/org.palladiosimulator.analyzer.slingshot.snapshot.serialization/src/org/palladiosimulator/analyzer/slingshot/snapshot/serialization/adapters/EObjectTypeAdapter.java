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
import org.palladiosimulator.commons.emfutils.EMFLoadHelper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

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

	@Override
	public void write(final JsonWriter out, final EObject value) throws IOException {		
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

	@Override
	public EObject read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		
		final String file = s.split("#")[0];
		
		if (URIMap.containsKey(file)) {
			final URI uri = URIMap.get(file).appendFragment(s.split("#")[1]);
			return EMFLoadHelper.loadAndResolveEObject(set, uri); 
		}
				
		return EMFLoadHelper.loadAndResolveEObject(set, s); 
	}
}
