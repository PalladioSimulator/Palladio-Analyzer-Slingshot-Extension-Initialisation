package org.palladiosimulator.analyzer.slingshot.snapshot.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.ClassTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.EObjectTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.TypeTokenTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.ReferenceEntity;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.SimpleEntity;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.GenericPCMEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.GenericPCMEvent2;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.LoopThingChild;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.LoopThingParent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.OptionalThing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.PCMEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.Thing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.exception.ModelElementWriteException;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.DESEventTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.ElistTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.EntityTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.OptionalTypeAdapterFactory;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

public class TestClass {
	
	EventCreationHelper helper = new EventCreationHelper();
	
	@Test
	public void foo() {

		
	}
	@Test
	public void testOptionalPCM() {
		final Gson writegson = new GsonBuilder()
				.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(helper.set))
				.registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of()))
				.create();
		final Gson readgson = new GsonBuilder()
				.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(helper.set))
				.registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of()))
				.create();

		final AbstractUserAction action = helper.createUsageModel().getUsageScenario_UsageModel().get(0)
				.getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour().get(0);
		
		final Optional<AbstractUserAction> optional = Optional.of(action);

		final JsonElement actual = writegson.toJsonTree(optional);
		
		assertTrue(actual.isJsonObject());
		assertTrue(actual.getAsJsonObject().has(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD));
		
		final JsonElement actualValue  = actual.getAsJsonObject().get(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD);
		

		assertTrue(actualValue.isJsonObject());
		assertTrue(actualValue.getAsJsonObject().has(OptionalTypeAdapterFactory.FIELD_NAME_CLASS));
		assertTrue(actualValue.getAsJsonObject().has(OptionalTypeAdapterFactory.REFERENCE_FIELD));
		assertTrue(actualValue.getAsJsonObject().get(OptionalTypeAdapterFactory.REFERENCE_FIELD).isJsonPrimitive());

		assertEquals(action.getClass().getCanonicalName(), actualValue.getAsJsonObject().get(OptionalTypeAdapterFactory.FIELD_NAME_CLASS).getAsString());
		assertEquals(writegson.toJsonTree(action).getAsString(), actualValue.getAsJsonObject().get(OptionalTypeAdapterFactory.REFERENCE_FIELD).getAsString());
		
		
		
		final Optional<AbstractUserAction> actualOptionalAction = readgson.fromJson(actual, new TypeToken<Optional<AbstractUserAction>>() {}.getType());
		
		assertTrue(actualOptionalAction.isPresent());
		assertEquals(action, actualOptionalAction.get());
	}
	
	@Test
	public void testOptionalEmpty() {
		final Gson writegson = new GsonBuilder().registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();
		final Gson readgson = new GsonBuilder().registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();

		
		final Optional<SimpleEntity> optional = Optional.empty();

		final JsonElement actual = writegson.toJsonTree(optional);
		assertTrue(actual.isJsonObject());
		
		final JsonObject actualObj = actual.getAsJsonObject();
		assertTrue(actualObj.has(OptionalTypeAdapterFactory.FIELD_NAME_CLASS));
		assertTrue(actualObj.has(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD));
		
		assertEquals(OptionalTypeAdapterFactory.OPTIONAL_EMPTY, actualObj.get(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD).getAsString());
		
		final Optional<SimpleEntity> actualOptional = readgson.fromJson(actual, new TypeToken<Optional<SimpleEntity>>() {}.getType());
		
		assertTrue(actualOptional.isEmpty());
	}
	
	@Test
	public void testOptionalWithReference() {
		final Gson writegson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();
		final Gson readgson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();
		
		final SimpleEntity entity = new SimpleEntity("entity");
		final Optional<SimpleEntity> optional = Optional.of(entity);
		
		final JsonElement actualEntity = writegson.toJsonTree(entity);
		final JsonElement actualOptional = writegson.toJsonTree(optional);
		
		assertTrue(actualEntity.isJsonObject());
		final JsonObject actualEntityObj = actualEntity.getAsJsonObject();
		assertTrue(actualEntityObj.has(EntityTypeAdapterFactory.FIELD_NAME_ID_FOR_REFERENCE));

		
		assertTrue(actualOptional.isJsonObject());
		final JsonObject actualOptionalObj = actualOptional.getAsJsonObject();
		
		assertTrue(actualOptionalObj.has(OptionalTypeAdapterFactory.FIELD_NAME_CLASS));
		assertTrue(actualOptionalObj.has(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD));
		
		assertTrue(actualOptionalObj.get(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD).isJsonObject());
		final JsonObject actualOptionalValueObj = actualOptionalObj.get(OptionalTypeAdapterFactory.OPTIONAL_VALUE_FIELD).getAsJsonObject();
		
		
		assertTrue(actualOptionalValueObj.has(OptionalTypeAdapterFactory.FIELD_NAME_CLASS));
		assertTrue(actualOptionalValueObj.has(OptionalTypeAdapterFactory.REFERENCE_FIELD));
		
		assertEquals(actualEntityObj.get(EntityTypeAdapterFactory.FIELD_NAME_ID_FOR_REFERENCE), actualOptionalValueObj.get(OptionalTypeAdapterFactory.REFERENCE_FIELD));
		
		
		final SimpleEntity actualReadEntity = readgson.fromJson(actualEntity,new TypeToken<SimpleEntity>() {}.getType());
		final Optional<SimpleEntity> actualReadOptional = readgson.fromJson(actualOptional, new TypeToken<Optional<SimpleEntity>>() {}.getType());
		
		assertTrue(actualReadOptional.isPresent());
		assertTrue(actualReadEntity == actualReadOptional.get()); // actual reference equality
		
	}
		
	/**
	 * Check consistency of references for (de-)serialization of an entity with the {@link EntityTypeAdapterFactory}.
	 */
	@Test
	public void testEntityReferenceConsistency() {
		final Gson writegson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}, new TypeToken<ReferenceEntity>() {}))).create();
		final Gson readgson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}, new TypeToken<ReferenceEntity>() {}))).create();

		final SimpleEntity simple = new SimpleEntity("value");
		final ReferenceEntity entity = new ReferenceEntity(simple, simple);
	
		final String actual = writegson.toJson(entity);
		
		assertEquals(2, actual.split("value").length); // "value" appears only once, as the second appearance is a reference id.
		
		final ReferenceEntity actualEntity = readgson.fromJson(actual, ReferenceEntity.class);
		
		assertEquals(entity, actualEntity);
		assertTrue(actualEntity.getEntity1() == actualEntity.getEntity2());		
	}
	
	/**
	 * Check correct (de-)serialization of a simple entity with the {@link EntityTypeAdapterFactory}.
	 */
	@Test
	public void testSimpleEntity() {
		final Gson writegson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();
		final Gson readgson = new GsonBuilder().registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(new TypeToken<SimpleEntity>() {}))).create();

		
		final SimpleEntity entity = new SimpleEntity("value");
		
		final JsonElement actual = writegson.toJsonTree(entity);
		assertTrue(actual.isJsonObject());
		
		final JsonObject actualObj = actual.getAsJsonObject();
		assertTrue(actualObj.has(EntityTypeAdapterFactory.FIELD_NAME_ID_FOR_REFERENCE));
		
		assertTrue(actualObj.has(EntityTypeAdapterFactory.FIELD_NAME_CLASS));
		assertEquals("\"" + entity.getClass().getCanonicalName() + "\"", actualObj.get(EntityTypeAdapterFactory.FIELD_NAME_CLASS).toString());
		
		assertTrue(actualObj.has(EntityTypeAdapterFactory.FIELD_NAME_OBJECT));
		assertTrue(actualObj.get(EntityTypeAdapterFactory.FIELD_NAME_OBJECT).isJsonObject());
	
		final SimpleEntity actualEntity = readgson.fromJson(actual, SimpleEntity.class);
		
		assertEquals(entity, actualEntity);
	}
	
	/**
	 * test one of the events with type generics. 
	 */
	@Test
	public void testGenericDESEvent() {
		// TODO, e.g. Abstract Entity Changed
		
	}
	
	/**
	 * Check wheter serialisation of a simple DES event has corrects fields, and
	 * whether deserialisation of the very event has the correct values again.
	 */
	@Test
	public void testSimpleDESEvent() {
		final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new DESEventTypeAdapterFactory(Set.of())).create();
		
		final DESEvent event = new SimulationStarted();
		
		final JsonElement actual = gson.toJsonTree(event);
		
		assertTrue(actual.isJsonObject());
		
		final JsonObject actualObj = actual.getAsJsonObject();
		
		assertTrue(actualObj.has("type"));
		assertEquals("\"" + event.getClass().getCanonicalName() + "\"", actualObj.get("type").toString());
		
		assertTrue(actualObj.has("event"));
		assertTrue(actualObj.get("event").isJsonObject());
		
		final JsonObject actualEventObj = actualObj.get("event").getAsJsonObject();
		
		assertTrue(actualEventObj.has("scheduledTime"));
		assertTrue(actualEventObj.has("delay"));
		assertTrue(actualEventObj.has("id"));

		final DESEvent actualEvent = gson.fromJson(actual, SimulationStarted.class);
		
		// eqauls is not overwritten, must compare field by field
		assertEquals(event.getId(), actualEvent.getId());
		assertEquals(event.getName(), actualEvent.getName());
		assertEquals(event.time(), actualEvent.time());
		assertEquals(event.delay(), actualEvent.delay());
	}
	
	@Test
	public void testElistTypeAdapterEmptyList() throws IOException {
		final EList<EObject> list = new BasicEList<>();

		// go via json, or we wont get the correct delegate for the list content.
		final Gson gson = new GsonBuilder()
				.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(helper.set))
				.registerTypeAdapterFactory(new ElistTypeAdapterFactory()).create();

		assertEquals("[]", gson.toJson(list));
		assertTrue(((EList<EObject>) gson.fromJson("[]", new TypeToken<EList<EObject>>() {}.getType())).isEmpty());
		
	}

	@Test
	public void testElistTypeAdapter() throws IOException {
		// go via json, or we wont get the correct delegate for the list content.
		final Gson gson = new GsonBuilder()
				.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(helper.set))
				.registerTypeAdapterFactory(new ElistTypeAdapterFactory()).create();

		final UsageModel model = helper.createUsageModel();		
		final EList<AbstractUserAction> actionslist = model.getUsageScenario_UsageModel().get(0).getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour();
		final String elementId = "id";
		
		for (int i = 0; i < actionslist.size(); i++) {
			actionslist.get(i).setId(elementId + i);
		}
		
		final String actual = gson.toJson(actionslist);

		assertEquals('[', actual.charAt(0));
		assertEquals(']', actual.charAt(actual.length()-1));
		
		for (int i = 0; i < actionslist.size(); i++) {
			assertTrue(actual.contains(elementId+i));
		}
		
		assertIterableEquals(actionslist, ((EList<EObject>) gson.fromJson(actual, new TypeToken<EList<EObject>>() {}.getType())));		
	}
	
	/**
	 * Check that model elemets are correctly read and written.
	 */
	@Test
	public void testEObjectAdapter() throws IOException {
		final String prefix = "/some/path/to/some/where/";
		final String filelocation = "foo.bar";
		final String elementId = "id";
		final String expected = "\"" + filelocation + "#" + elementId + "\"";
		
		final UsageModel model = helper.createUsageModel();	
		
		final AbstractUserAction action = model.getUsageScenario_UsageModel().get(0).getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour().get(0);
		action.setId(elementId);
		
		model.eResource().setURI(URI.createFileURI(filelocation));
		assertEquals(expected, (new EObjectTypeAdapter(helper.set)).toJson(action));
		assertEquals(action, (new EObjectTypeAdapter(helper.set)).fromJson(expected));
		
		model.eResource().setURI(URI.createFileURI(prefix + filelocation));
		assertEquals(expected, (new EObjectTypeAdapter(helper.set)).toJson(action));
		assertEquals(prefix + filelocation, (new EObjectTypeAdapter(helper.set)).fromJson(expected).eResource().getURI().toFileString());
	}
	
	/**
	 * Check that model without resource throw an exception.
	 */
	@Test
	public void testModelElementWriteException() {
		final UsageModel model = helper.createUsageModel();	
		model.eResource().getContents().clear();
		
		assertThrows(ModelElementWriteException.class, () -> (new EObjectTypeAdapter(helper.set)).write(null, model.getUsageScenario_UsageModel().get(0)));		
	}
	
	@Test
	public void testClassTypeAdpater() {
		assertEquals("\"" + String.class.getCanonicalName() + "\"", (new ClassTypeAdapter()).toJson(String.class));	
	}
	
	@Test
	public void testTypeTokenTypeAdpater() {
		assertEquals("\"" + String.class.getCanonicalName() + "\"", (new TypeTokenTypeAdapter()).toJson(new com.google.common.reflect.TypeToken<String>() {}));	
	}
	
	
	public void deserailize(final Set<DESEvent> events) {
		
		
		// shared data structures
		final Map<String, Object> objs = new HashMap<>();
		final Map<String, TypeAdapter<?>> thingTypes = new HashMap<>();
				
		final GsonBuilder adaptereBuilder = new GsonBuilder();
		adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(helper.set));
		adaptereBuilder.registerTypeHierarchyAdapter(com.google.common.reflect.TypeToken.class, new TypeTokenTypeAdapter());

		
		
		adaptereBuilder.registerTypeAdapterFactory(new EntityTypeAdapterFactory(Set.of(
				TypeToken.get(Thing.class), TypeToken.get(OptionalThing.class), TypeToken.get(LoopThingChild.class), TypeToken.get(LoopThingParent.class))));
		adaptereBuilder.registerTypeAdapterFactory(new OptionalTypeAdapterFactory(Set.of()));
		adaptereBuilder.registerTypeAdapterFactory(new ElistTypeAdapterFactory());
		
		adaptereBuilder.registerTypeAdapterFactory(new DESEventTypeAdapterFactory(Set.of()));
		
		
		final Gson gsonwithAdapter = adaptereBuilder.create();

		
		
	
		
		final Map<String, Class<? extends DESEvent>> eventTypes = new HashMap<>();
		eventTypes.put(SimulationStarted.class.getCanonicalName(), SimulationStarted.class);
		eventTypes.put(SimulationFinished.class.getCanonicalName(), SimulationFinished.class);
		eventTypes.put(PCMEvent.class.getCanonicalName(), PCMEvent.class);
		eventTypes.put(GenericPCMEvent.class.getCanonicalName(), GenericPCMEvent.class);
		eventTypes.put(GenericPCMEvent2.class.getCanonicalName(), GenericPCMEvent2.class);
		
		final String eventJsonString = gsonwithAdapter.toJson(events);		
		System.out.println(eventJsonString);
		
		final Type set2Type = new TypeToken<HashSet<DESEvent>>(){}.getType();

		final Set<DESEvent> results =  gsonwithAdapter.fromJson(eventJsonString, set2Type);
		
	}

}
