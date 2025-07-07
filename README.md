# Palladio-Analyzer-Slingshot-Extension-Initialisation

Slingshot Extension to resume a previous simulation run. 
It provides the means to save the state of a simulation run as a *snapshot*, and to initialise a simulation run from a given snapshot.
It does not hook into the graphical user interface of Palladio and is intended to be used for headless runs only. 

This extension was created for the MENTOR DFG project.  

## General Explanation

The over all goals of this extension are (1) to stop and restart a simulation at an arbitrary point in time and (2) to apply arbitrary scaling policies at the beginning of a simulation run.
Considering (2), it is important to note that the policies at the beginning are not applied according to their trigger conditions.
Instead, the user decides on their application upfront, and the extension applies (enforces) the policies in spite of trigger conditions and/or constraints. 

For stopping and restarting, the extensions saves/loads snapshots to/from a JSON file.
For applying scaling policies, the extension accepts an additional JSON file with a list of scaling policy ids as input. 

For the sake of simplification, the following explanation reduces the entire extension to four components.
These components are `application`,  `initialisation`, `simulator` and `snapshotting`. 

`application` is the entrypoint. 
It parses the commandline arguments and experiment settings (`*.experiments` file), and loads the PCM instances from the model files. 
Afterwards it starts `initialisation`.

`initialisation` loads the JSON-files with the snapshot and configurations, including the policies to be applied, and creates the result copy of the PCM instances. 
The simulation will later on use the result copies only, such that the initial PCM instances remain unchanged. 
Then `initialisation` provisions the snapshot, further configuration parameters and the policies to be applied to Slingshot and starts `simulator`. 

Slingshot is event based and event driven, for more details c.f. https://www.palladio-simulator.com/Palladio-Documentation-Slingshot/slingshot-simulator/. 
For now the following points are relevant: 
- Slingshot provides means to subscribe to events, and to pre- and postintercept events.  
- Various `SimulationBehaviorExtension` contribute behaviour to the simulation by subscribing to events and producing new events. 

Now, `simulator` loads all behaviour extensions, including those contributed by this extension's `snapshotting` component. 
Each of the behaviour extensions of `snapshotting` cover a distinct aspect of the snapshot mechanism. 
For now we will only focus on the injection of the snapshotted state at the beginning and the creation of a new snapshot at the end. 
For the sake of simplicity, we will call the former `initBehaviour` and the latter `stateUpdateBehaviour`

At first, `initBehaviour` preintercepts the `SimulationStarted` event and checks, whether the current simulation run should start with initialisation from snapshot, or not. 

If it should *not* start with initialisation from a snapshot, i.e. the simulation run actually starts at $t=0$, without any previous runs, the `SimulationStarted` event remains untouched and is delivered to the respective behaviours which then creates events according to the workload defined in the usage model.

If it should start with initialisation from a snapshot, i.e. the simulation run starts at a point in time $t > 0$, the `SimulationStarted` event is aborted.
Instead of creating new events according to the workload defined in the usage model, `initBehaviour` published the snapshotted events from the previous simulation run. 

In both cases, `initBehaviour` publishes events to trigger application for the scaling policies specified in the input JSON file. 

During the simulation, various other extensions of the `snapshotting` component continuously check, whether a snapshot should be taken.
Once any of the required conditions is met, `stateUpdateBehaviour` creates a snapshot and ends the simulation run by publishing a `SimulationFinished` event.  

Finally, `initialisation` postprocesses the simulation results, and saves the snapshot and the results to file. 
The results include the updated PCM instances, the measurements and additional information for putting the results into the state graph.  

## Structure of Repository

* `org.palladiosimulator.analyzer.slingshot.initialisedsimulation*`: 
  Bundles related to initialising, starting and postprocessing a simulation run. 
  Stuff from these bundles happens before and after the simulation run, but does not interfere with the simulation run itself. 
  * `*.application` : contains classes that are part of the `application` component.
  * `*.converter`: classes for converting the Palladio-specific EDP2 format to simple value pairs. 
    Measurements taken during a simulation run are initially saved in EDP2 but serialized to JSON as value pairs, using these classes.
  * `*.data`: data classes of the `initialisation` component.
  * `*.feature`: Feature project for exporting the entire Initialisation Extension as installable feature.
  * (no additional suffix): core of the `initialisation` component.
* `org.palladiosimulator.analyzer.slingshot.snapshot*`: 
  Bundles related to the snapshot. Mostly behaviours that interfere with the simulation be redirecting and/or injecting events, and the entire mechanism for taking the a snapshot. 
  * (no additional suffix): core fo the `snapshotting` component, especially the behaviour extensions
  * `*.data`: data classes of the `snapshotting` component.
  * `*.serialization`: classes for serializing a snapshot to JSON. 
    Depends on `com.google.gson`.
    The most important part of the snapshot -- and the most difficult to serialize -- are the instances of `DESEvent` that mirror the state of the `simulator` component.
    This bundle contains mostly adapters and factories for serializing different subclasses of `DESEvent` and the entities within.
* `org.palladiosimulator.analyzer.slingshot.behavior.util`: Utility classes. Small, but well used and important.   

## Format of input and output

* The PCM instances are expected in their usual XML-format, no changes at all. 
Notably, this extension's Application builds upon ExperimentAutomation, thus an experiment model (`*.experiment`-file) must be provided. 

* The JSON file for the snapshot uses the file-extension `.snapshot` and must adhere/adheres to the format of this example:
  ```
  {
    "id":"id-of-the-state-this-snapshot-was-taken-for",
    "pointInTime":0.0,
    "snapshot": {
      "events":[],
      "statevalues":[]
    }
  }
  ```

* The JSON file for the other information uses the file-extension `.config` and must adhere to the format of this example (further configuration option will be described later on):
  ```
  {
    "incomingPolicies": []
  }
  ```

* The JSON file for the results uses the file-extension `.state` and adheres to the format of this example:
  ```
  {
    "parentId":"id-of-the-parent-state",
    "id":"id-of-this-state",
    "startTime":0.0,
    "duration":110.24138783074197,
    "reasonsToLeave":["reactiveReconfiguration"],
    "utility":{
      "totalUtility":23.243757568282582,
      "data":[
        {
          "id":"_BGErMNV-EeSaPsLvWUqTbQ",
          "utility":152.0801793323635,
          "type":"SLO"
        },
        {
          "id":"_7QlDoXTYEe-c5L9OU_UA0Q",
          "utility":112.0,
          "type":"COST"
        }
      ]
    },
    "outgoingPolicyIds":["_xlJ0UnQhEe-xS75-6HAwnQ"],
    "measurementSets":[...]
  }
  ```
### Example Files
A complete set of PCM and JSON files can be found in [EspresssoAccountingMinimalExample](https://github.com/meccr/example-models/tree/main/EspresssoAccountingMinimalExample).

* The folder `input` contains the JSONs for starting a simulation run at $t=0$ with or without policy application. 

* The folder `output` contains the PCM copies and JSONs created after initialising a simulation on an empty snapshot and apply no policies at the beginning.
  * Beware, the  PCM files should be identical to the ones in the parent folder. 
  * Files and folders used as arguments:
    * model files: those in `EspresssoAccountingMinimalExample`
    * snapshot file: `EspresssoAccountingMinimalExample/input/snapshot.json`
    * config file: `EspresssoAccountingMinimalExample/input/config.json`
    * output folder: `EspresssoAccountingMinimalExample/output`

* The folder `output2` contains the PCM copies and JSONs created after initialising a simulation on the snapshot from the previous run (`output`) and apply a policies at the beginning. 
  * Beware, the  PCM files should now differ from the ones in the `output` folder, as the applied policy was a scale out.  
  * Files and folders used as arguments:
    * model files: those in `EspresssoAccountingMinimalExample/output`
    * snapshot file: `EspresssoAccountingMinimalExample/output/snapshot.json`
    * config file: `EspresssoAccountingMinimalExample/input/applyPolicyConfig.json`
    * output folder: `EspresssoAccountingMinimalExample/output2`

## Dev Setup

**Beware:** This extension is intended for running headless, thus you may exclude all `*.edit` and `*.editor` bundles in the following steps. 

* Follow the steps in the normal Slingshot Documentation (https://palladiosimulator.github.io/Palladio-Documentation-Slingshot/tutorial/) to setup Slingshot and maybe run an (normal) example. 
Pay attention to also import the SPD meta model and the SPD interpreter extension.
  
* Clone [Palladio-Analyzer-Slingshot-Extension-Initialisation](https://github.com/PalladioSimulator/Palladio-Analyzer-Slingshot-Extension-Initialisation)
  ```
  git clone git@github.com:PalladioSimulator/Palladio-Analyzer-Slingshot-Extension-Initialisation.git
  ```
* In (already cloned) repository *Palladio-Analyzer-Slingshot-Extension-SPD-Interpreter* switch branch 
  ```
  git checkout stateexplorationRequirements
  ```
  * This is necessary, because the SPD interpreter extension is stateful but has no hook to initialise its state from the outside. 
    On this initialisation specific branch (the name is legacy) we added the possibility to initialise the state to an arbitrary value. 
    This change was not accepted into the `master` branch, because it does not adhere to the current design of the slingshot event cycle.   

+ Import Experiment-Automation bundles. 
  * clone repository [Palladio-Addons-ExperimentAutomation](https://github.com/PalladioSimulator/Palladio-Addons-ExperimentAutomation) and switch to branch `slingshot-impl`:
  ```
  git clone git@github.com:PalladioSimulator/Palladio-Addons-ExperimentAutomation.git
  git checkout slingshot-impl
  ```
  * import the bundles `org.palladiosimulator.experimentautomation` and `org.palladiosimulator.experimentautomation.application.tooladapter.slingshot.model` into the workspace of your development Eclipse instance.
  Ignore all other bundles. 

## Running an initialised Slingshot simulation run
The following subsections describe two approaches run an initialised simulation run.
Both of them require a set of **models and JSON files**.

### Run from within the development Eclipse instance
* **Requires: all bundles described in [Dev Setup](#Dev-Setup) imported into eclipse workspace**
+ Create an *OSGi Framework* Run Configuration.
  * Exclude all `*.edit`, `*.editor` and `*.ui` bundles. 
    * Excluding `*.edit` is not required, but we don't need it.
    * Only exclude `*.ui`. The `*.ui.events` are still needed.   
  * Uncheck *Include optional dependencies [...]* 
  * Click *Add Required Bundles*, to add dependencies
  * Click *Validate Bundles*, just to be on the safe side.
  * Go to tab *Arguments*
  * Add `-application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication` to *Program arguments*
  * Append further application arguments to *Program arguments*, details see [section on specifying application arguments](#Specifying-Application-Arguments).
  * Remove `-Declipse.ignoreApp=true` from *VM arguments* or else the application will be ignored.
  * (Optional) Add `-Dlog4j.configuration=file:///path/to/log4j.properties` to *VM arguments* to specify your own logging properties. 
  * Run it.


### Run from Commandline
* **Requires: PalladioBench with Initialisation Extension already installed, for more details see [Manual Export and Installation](#Manual-Export-and-Installation)**
* execute
  ```
  ./PalladioBench -data /path/to/workspace/ \
  -application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
  [application arguments]
  -vmargs -Xmx4G -Dlog4j.configuration=file:///path/to/log4j.properties
  ```
### Specifying Application Arguments

#### General Arguments:
* `-id` (*Optional*): Will be used as id for the resulting state of the simulation run. If none is specified, a random id will be used. 
* `-output` (*Mandatory*): Destination folder for saving the results.
* `-input`: Source for loading the snapshot-, config-, and experiments-file, if no other information about their location is provided.
If snapshot-, config-, and experiments-file are specified with the specific options (see below), this option is *optional*. 
For this option, the application matches the files by file extension, i.e. `.snapshot` for the snapshot-file, `.config` for the config file, and `.experiments` for the experiments-file. 
Each extension must appear at max once in the input folder, or else the application cannot identify the file to be used.


#### Specific Input Arguments:
These options take precedence over the `-input` option, e.g., if both the `-input` and the `-snapshot` options are specified, the application loads the snapshot from the location specified by the latter. 
They have no preconditions on the file extensions, but require the locations to be absolut paths.
* `-snapshot`: Location of the snapshot-file. 
* `-experiments`: Location of the experiments-file. 
* `-config`: Location of the config-file.

#### Specific Output Arguments:
Theses options are optional. 
They cannot replace the `-output` option.
They accept locations as absolute or relative paths.
Relative paths are resolved against the path specified with the `-output` option.
* `-outputSnapshot`: Location of the snapshot-file created after the simulation run. 
Defaults to `/path/to/output/filename-of-input-snapshot`.
* `-outputState`: Location of the state-file created after the simulation run. 
Defaults to `/path/to/output/id-of-state.state`.



#### Least amount of arguments Example (for CLI execution)
```
./PalladioBench -data /path/to/workspace/ \
-application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
-output /absolute/path/to/output/directory \
-input /absolute/path/to/input/directory \
-vmargs -Xmx4G -Dlog4j.configuration=file:///path/to/log4j.properties
```
* `/absolute/path/to/input/directory` must contains exactly one `.snapshot`-file, exactly one `.experiments`-file and  exactly one `.config`-file.
* The id of the new state will be an random generated UUID.
* All results will be saved to `/absolute/path/to/output/directory`, using their default filenames.
* The experiments- and snapshot-file may contain reference to PCM instances as absolute or relative paths. 
The user must ensure, that the PCM instances are at the expected locations, otherwise the application will fail.  

#### More elaborate Example (for CLI execution)
```
./PalladioBench -data /path/to/workspace/ \
-application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
-id some-string-id \
-output /absolute/path/to/output/directory \
-input /absolute/path/to/input/directory \
-config /absolut/path/to/config.json \
-outputSnapshot snapshot/foo.json
-vmargs -Xmx4G -Dlog4j.configuration=file:///path/to/log4j.properties
```
* `/absolute/path/to/input/directory` must contains exactly one `.snapshot`-file and exactly one `.experiments`-file, because no other location is specified.
* `/absolute/path/to/input/directory` may also contain `.config`-files, but they are ignored in favor of the file specified by `-config`.
* The given location for the result snapshot is relative, the snapshot will be saved to `/absolute/path/to/output/directory/snapshot/foo.json`.
* No location for the result state is given, it will be saved to `/absolute/path/to/output/directory/some-string-id.state`.

## Manual Export and Installation

**Requires: all bundles described in [Dev Setup](#Dev-Setup) imported into the workspace**

### Export Feature
1. open development eclipse instance with all bundles described above imported and a correctly set target platform. 
2. export the initialisation extension as feature:
    * `export` $\rightarrow$ `Deployable Features`
    * select `org.palladiosimulator.analyzer.slingshot.initialisedsimulation.feature (1.0.0.qualifier)` $\rightarrow$ `finish`
    * This is an all-in-one feature that just includes *everything* without any structure at all. Enjoy with caution.  

### Import Feature
1. install a fresh PalladioBench (https://updatesite.palladio-simulator.com/palladio-bench-product/releases/latest/) and open it.
6. install the feature exported in step **Export Feature** (see above):
    * `Help` $\rightarrow$ `Install New Software` $\rightarrow$ `Add...`
    * In the pop-up click `Local...` and choose the folder with the manually exported feature.
    * untick `Group items by category`. A Plug in  named `Initialised Simulation` becomes visible. Select it and finish installation. 
7. close the PalladioBench
8. Checks successful installation: 
    * Run PalladioBench from CLI with `--console` to get an OSGi console.
    * Execute `lb` to list installed Plugins. 
    * Search for `Initialised Simulation Application`. If it is there, and the state is `Starting` everything is fine.

### Create a Palladio Bench for running in the Docker Container.
**Requires: The Docker Container will be a Linux system. I do not know whether the following approach works with anything other but a Linux systems.**

1. Get a Linux system, either an actual machine or a VM (i did the latter) and set it up to run a PalladioBench, e.g. by installing a fitting java version.
2. Install a fresh PalladioBench (https://updatesite.palladio-simulator.com/palladio-bench-product/releases/latest/) and open it.
3. Import Slingshot core, and load the target platform. 
   This step is necessary for loading some global dependencies, i guess. In fact, i only know, that the following steps failed for me, if i did not set the targetplatform upfront.
4. execute the **Import Feature** (see above) steps on the Linux system.
5. open `PalladioBench.ini` and remove the following lines: 
   ```
   -perspective 
   org.palladiosimulator.pcmbench.perspectives.palladio
   ```
6. copy the entire Palladio Bench folder to the Docker container.

## Advanced Configuration: Deactivate Snapshot-related Behaviour Extensions.

The `snapshotting` component (c.f. [Section General Explanation](#General-Explanation)) consists of multiple separate behaviour extension classes. 
For technical details on Slingshot's behaviour read the [Simulator's Documentation](https://www.palladio-simulator.com/Palladio-Documentation-Slingshot/slingshot-simulator/).

Currently (June '25), `snapshotting` contributes 11 behaviour extensions. 
Each extension takes care of a distinct aspect of the initialisation and snapshotting mechanism. 
Some extensions accept additional configurations, including deactivation.
To do additional configurations, the `.config` file must be extended.

The following sections describe the configurable extensions, and the required additions to the `.config` file.

### Non-essential behaviour extensions
Currently (June '25), the following behaviour extension are configurable:

* `SnapshotTriggeringBehavior` 
  - Triggers a snapshot if any policies' trigger fired. 
  - As an additional feature, this behaviour drops (i.e. does not trigger a snapshot) scale ins adjustments on minimal architecture configurations. 
    To deactivate this feature, add `"doDrop" : false` to the `.config` file. 
* `SnapshotSLOTriggeringBehavior`
  - Triggers a snapshot if a measurement is close to an SLO threshold.
  - The "closeness" is configurable. 
    By adding the parameter `"sensitivity" : 0` to the `.config` file, snapshots are triggered only, if measurements are greater or equal to the threshold.
    This is also the default behaviour. 
    By increasing the sensibility value, snapshots are triggered earlier. 
    The maximum sensibility is `"sensitivity" : 1`. 
* `SnapshotSLOAbortionBehavior`
  - Aborts a simulations run, if a hard SLO threshold is violated. 
    The output of the simulation run still contains a snapshot and a sate, but the state will be marked as `aborted`.
  - This extension can be deactivated by adding `"active" : false` to the `.config` file. 
* `SnapshotAbortionBehavior`
  - Aborts a simulations run, if scaling policies were enforced at the beginning of the run, but the policies yielded no architecture changes. 
    This happens, e.g., if a scale in and a scale out policy are applied simultaneously, and their effects cancel each other out. 
    The output of the simulation run still contains a snapshot and a sate, but the state will be marked as `aborted` and also empty. 
  - This extension can be deactivated by adding `"active" : false` to the `.config` file. 

### Extending the `.config` file

To further configure the configurable extension, add a field `parameters`, that associates a configurable extensions with a map of configuration parameters and values. 
By default configurable behaviour extension are identified by class name.
Deviating identification names should be documented and must be looked up in the JavaDoc.

All configurable extension accept the parameter `"active"` for deactivating the extension. 
Unless specified otherwise, all extensions are active. 
Additional available parameters per extension must be looked up in the JavaDoc. 

Example for configuring the behaviour extension `SnapshotSLOTriggeringBehavior`:

```
{
"incomingPolicies": [],    
"parameters" : {
    "SnapshotSLOTriggeringBehavior" : { "active" : true, "sensitivity" : 0.5 }
  }
}
```

*  
  The key `"SnapshotSLOTriggeringBehavior"` identifies the following map as configuration parameters for the class with that very name.
* All configurable extensions can be deactivated by adding the "*active*" parameter and setting the value to `false`.
  By default, all configurable extensions are activated.
  Thus explicitly activating an extension, as done in this example, is not necessary, but may be helpful for understandability.
* The `SnapshotSLOTriggeringBehavior` accepts an additional parameter "*sensitivity*" that must be a double value in [0,1]. 
  Each behaviour extension defines different additional parameters, check the documentation of the classes, to learn about the available configuration parameter.

Another Example:
```
{
  "incomingPolicies": [],    
  "parameters" : {
    "SnapshotTriggeringBehavior" : { "active" : true, "doDrop" : false},
    "SnapshotSLOTriggeringBehavior" : { "active" : false, "sensitivity" : 0},
    "SnapshotSLOAbortionBehavior" : { "active" : false},
    "SnapshotAbortionBehavior" : { "active" : false}
  }
}
```
* Deactivate all configurable behaviours, except for `SnapshotTriggeringBehavior`.
* The parameter `"sensitivity"` has no effect, because the respective extension is already deactivated.
* Do not drop anything, when triggering snapshots with `SnapshotTriggeringBehavior`

### Adding your own configurable behaviour extension

All configurable behaviour extension should extend the abstract class `ConfigurableSnapshotExtension`.
The class already implements the interface `SimulationBehaviorExtension`. 
For further information confer the JavaDoc.



## Development Details : Snapshot (De-)Serialisation.

Serializing and deserializing a snapshot to JSON required several design decisions. 
Most of them depend on the structure of Slingshot's events and entities, thus this section starts of with a brief dive into that topic, before elaborating on the actual serialisation.

### Slingshot's Events and Entities

The Slingshot simulator sports an event-oriented worldview.
Everything that happens during a simulation, such as a user entering the system, or the processing of a request finishing, is announced with an event. 
For more details on the events checkout the [Simulator's Documentation](https://www.palladio-simulator.com/Palladio-Documentation-Slingshot/slingshot-simulator/)

Most events, especially those related to specific users, hold entities that represent the current context for simulating a user. 
The entities are nested structures and are getting more complex the further the simulation progresses. 

The figure below shows a simplified excerpt of the entities' class graph.
The most important points are: 
1. There are class hierarchies and information about the dynamic type of references must be maintained, or else the deserialization is impossible.
2. Slingshot's entities reference each other, and references must be kept. 
   E.g. both `RequestProcessingContext` and `UserInterpretationContext` reference the same instance of `User`.
3. There are circular references.
   E.g. the `behavior`/`context`relation between `SeffBehaviorWrapper` and `SeffBehaviorContextHolder`.
4. Many entities reference elements from the PCMs. 
   The PCM elements are `EObjects` and need special treatment.

Additional points, that caused problems during implementation: 

5. There are `Optional`s, that need special treatment.
6. There are `EList`s, that need special treatment.
7. Some event types hold additional information about generic types, that would otherwise be lost due to type erasure. These fields of type `TypeToken` and `Class` need special treatment.

### 1. Maintaining information about the dynamic types

Each event and entity is always serialised with an additional field that hold the type information. 
For an event, it may look like this:
```
{
  "type": "org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated",
  "event": { ... }
}
```
* `type` contains the canonical name of the event's dynamic type.
* `event` contains the serialization of the actual event. 

For an entity, it may look like this:
```
{
  "class": "org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext",
  "refId": "227998366$393118622",
  "obj": { ... }
}
```
* `class` contains the canonical name of the entity's dynamic type.
* `refId` will be explained in the next section.
* `obj` contains the serialization of the actual entity.

For deserialization, the respective type adapter factories maintain a mapping from dynamic types to specific type adapters for delegation. 
The mapping is created based on predefined sets of types.
Those sets are defined in `org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.SlingshotTypeTokenSets`.
(De-)Serializing any event or entity whose type is not listed in those sets might result in errors or unexpected behaviour.

### 2. Keep the references between entities
As mentioned in the previous section, a serialized entity may look like this: 
```
{
  "class": "org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext",
  "refId": "227998366$393118622",
  "obj": { ... }
}
```
* `class` and `obj`: see previous section.
* `refId` id of the entity. 
  The id is calculated in `org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.SnapshotSerialisationUtils#getReferenceId`. 
  Confer the JavaDoc for more details.  
  
However, the serialized entity only looks like this the first time it occurs. 
All subsequent occurrences are references only, e.g. the serialisation of a `SEFFModelPassedElement` event that get serialized after the entity shown above, but holds a references to the entity in the field `context` looks like this: 

```
{
  "type": "org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement",
  "event": {
    "context": "227998366$393118622",
          ...
  }
}
```
* `context`: instead of serializing the entire `SEFFInterpretationContext` entity as shown in the previous snippet again, only the id is written. 

During Serialisation, the entities' type adapters maintain a data structure to track which entities are already serialized.
During Deserialisation, the entities' type adapters maintain a data structure to resolve reference ids to already deserialized entities.

Notably, this approach cannot handle circular references.
This approach requires that all references in the JSON occur after the actual serialisation of an entity, which is not given in case of circular references. 
Regarding the resolution of this problem, confer the next section.     

### 3. Circular References 

Slingshot's entities graph contains circles, which cannot be deserialized with the reference id approach described in the previous section.
Serialisation works just fine though.

Currently, the only known (and handled) circle exists between `SeffBehaviorWrapper` and `SeffBehaviorContextHolder`, where the wrapper references the context holder as `context` and the context holder has field `behaviors` that is a list of wrappers.

The additional two additional type adapters resolve the circle between wrapper and context holders. 
When reading a JSON, they delegate the deserialisation to the "normal" entity type adapter. 
If the "normal" type adapter encounters an unknown reference id, it returns `null`. 
Thus, next the circle-resolving adapters check whether the fields that form the circle are `null`.
If the fields are not `null` – i.e. the other half of the circle is already (incompletely) deserialized – the circle-resolving adapters use reflections to set the references, such that the circle is complete. 

### 4. PCM elements

The PCM instances use their own XML format for serialisation.
We decided, that translating or including the XML-serialisations into the JSON files is a bad idea.
Instead we decided to serialise all PCM elements as String representation fo the model element. 
E.g. the serialisation of an `ActiveJob` entity looks like this:
```
{
  "processingResourceType": "pathmap://PCM_MODELS/Palladio.resourcetype#_oro4gG3fEdy4YaaT-RYrLQ",
  "allocationContext": "default.allocation#_nzKigFWREfChJLy4meFBug",
  "request": { ... }
  ...
}
```
An `ActiveJob` entity has fields `processingResourceType` and `allocationContext` that reference PCM elements.
For PCM elements, that have a files resources, such as `allocationContext` in the snippet above, only the file name and the PCM element's fragment is written. 
For all other PCM elements, such  as `processingResourceType` in the snippet above, the entire URI is written. 

For PCM elements, that have a files resources, file name and fragment is enough to get the correct object, because for each simulation run, we already have an experiments file that holds the information about the exact location of the models. 

Thus the precondition for deserializing PCM elements are:

1. PCM instances are already loaded. 
2. The experiments file and the snapshot originate from the same simulation run, i.e. reference the same PCM instances.  

### 5. `Optional`

If i recall correctly, we required a special adapter for optionals, because otherwise the type of the optional's value would not be recognized correctly. 
Also, there were some difficulties with empty optionals, and fields of type `Optional` with value `null`.

### 6. `EList`

Some entities use `EList` as collection type.
Sadly, gson's normal collection adapters could not handle `EList`s.

### 7. `TypeToken` and `Class`

Some events and entities have fields of type `TypeToken` or `Class`.
Those fields are mostly necessary to retain some types that would otherwise be erased by type erasure. 

The values of those fields are serialized as canonical names:
```
{
  "type": "org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement",
  "event": {
    "context": "238194056$393118622",
    "genericTypeToken": "org.palladiosimulator.pcm.seff.impl.StartActionImpl",
    ...
  }
}
```

## Development Details : Noninvasively Recreating the State of `ResourceSimulation`.

## Development Details : Invasively Recreating the State of the SPD adjustor contexts.
