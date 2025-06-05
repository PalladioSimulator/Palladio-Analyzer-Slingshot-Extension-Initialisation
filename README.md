# Palladio-Analyzer-Slingshot-Extension-Initialisation

Slingshot Extension to resume a previous simulation run. 
It provides the means to save the state of a simulation run as a *snapshot*, and to initialise a simulation run from a given snapshot.
It does not hook into the graphical user interface of Palladio and is intended to be used for headless runs only. 

This extension was created for the MENTOR DFG project.  

## General Explanation about how it works.

The over all goals of this extension, are (1) to stop and restart a simulation at an arbitrary point in time and (2) to apply scaling policies at the beginning of a simulation run.

For stopping and restarting, the extensions saves/loads snapshots to/from a JSON file.
For applying scaling policies, the extension accepts an JSON with a list of scaling policy ids as input. 
For further detail, see section TODO below.  


For the sake of simplification, the following explanation reduces the entire machination to three components.
These components are `application`,  `initialisation`, `simulator` and `snapshotBehaviour`. 

`application` is the entrypoint. 
It parses the commandline arguments and experiment settings, and loads the PCM instances. 
Afterwards it starts `initialisation`.

`initialisation` loads the JSON-files with the snapshot and the policies to be applied and creates the result copy of the PCM instances. 
The simulation will later on use the result copies only, such that the initial PCM instances remain unchanged. 
Then `initialisation` provisions the snapshot, the policies to be applied and a results builder to Slingshot and starts `simulator`. 

Slingshot is event based and event driven, for more details c.f. https://www.palladio-simulator.com/Palladio-Documentation-Slingshot/slingshot-simulator/. 
For now the following points are relevant: 
- Slingshot provides means to subscribe to events, and to pre- and postintercept events.  
- Various `SimulationBehaviorExtension` contribute behaviour to the simulation by subscribing to events, and producing new events. 

Now, `simulator` loads all behaviour extensions, including the `snapshotBehaviour` contributed by this extension. 
In reality, `snapshotBehaviour` consists of many separate behaviour extensions that each cover a distinct aspect of the entire snapshot mechanism. 
For now we will only focus on the injection of the snapshotted state at the beginning and the creation of a new snapshot at the end. 

At first, `snapshotBehaviour` preintercepts the `SimulationStarted` event and checks, whether the current simulation run should start with initialisation from snapshot, or not. 

If it should *not* start with initialisation from a snapshot, i.e. the simulation run actually starts at $t=0$, without any previous run, the `SimulationStarted` event remains untouched and is delivered to the respective behaviours which then creates events according to the workload defined in the usage model.

If it should start with initialisation from a snapshot, i.e. the simulation run starts at a point in time $t > 0$, the `SimulationStarted` event is aborted. Instead of creating new events according to the workload defined in the usage model, `snapshotBehaviour` published the snapshotted events from the previous simulation run. 

In both cases, `snapshotBehaviour` publishes events to request adjustment for any scaling policies specified in the input JSON file. 

During the simulation, `snapshotBehaviour` continuously checks, whether a snapshot should be taken.
Once any of the required conditions is met, `snapshotBehaviour` takes a snapshot and ends the simulation run by publishing a `SimulationFinished` event.  

Finally, `initialisation` postprocesses the simulation results, and saves the snapshot and the results to file. 
The results include the updated PCM instances, the measurements and additional information for putting the results into the state graph.  

## Structure of Repository

* `org.palladiosimulator.analyzer.slingshot.initialisedsimulation*`: 
  Bundles related to initialising, starting and postprocessing a simulation run. 
  Stuff from these bundles happens before and after the simulation run, but does not interfere with the simulation run itself. 
* `org.palladiosimulator.analyzer.slingshot.snapshot*`: 
  Bundles related to the snapshot. Mostly behaviours that interfere with the simulation be redirecting and/or injecting events, and the entire mechanism for taking the a snapshot. 
* `org.palladiosimulator.analyzer.slingshot.behavior.util`: Utility classes. Small, but well used and important.   

## Format of input and output

* The PCM instances are expected in their usual XML-format, no changes at all. 
Notably, this extensions Application builds upon ExperimentAutomation, thus an experiment model must be provided. 

* The JSON file for the snapshot must adhere/adheres to the format of this example:
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

* The JSON file for the other information must adhere to the format of this example:
  ```
  {
    "sensibility": 0.0,
    "incomingPolicies": []
  }
  ```

* The JSON file for the results information adheres to the format of this example:
  ```
  {
    "parentId":"id-of-the-state-this-snapshot-was-taken-for",
    "id":"17152565-f337-4fba-943a-7b7ca5216277",
    "startTime":0.0,
    "duration":110.24138783074197,
    "reasonsToLeave":["reactiveReconfiguration"],
    "utility":{
      "totalUtility":23.243757568282582,
      "data":[...],
      "slo":[],
      "costs":[]
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
  * Arguments:
    ```
    -application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
    /path/to/example/EspresssoAccountingMinimalExample/espresso.experiments \
    /path/to/example/EspresssoAccountingMinimalExample/input/snapshot.json \
    /path/to/example/EspresssoAccountingMinimalExample/input/config.json \
    /path/to/example/EspresssoAccountingMinimalExample/output
    ```

* The folder `output2` contains the PCM copies and JSONs created after initialising a simulation on the snapshot from the previous run (`output`) and apply a policies at the beginning. 
  * Beware, the  PCM files should now differ from the ones in the `output` folder, as the applied policy was a scale out.  
  * Arguments:
    ```
    -application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
    /path/to/example/EspresssoAccountingMinimalExample/output/espresso.experiments \
    /path/to/example/EspresssoAccountingMinimalExample/output/snapshot.json \
    /path/to/example/EspresssoAccountingMinimalExample/input/applyPolicyConfig.json \
    /path/to/example/EspresssoAccountingMinimalExample/output2
    ```

## Dev Set up

**Beware:** This extension is intended for running headless, thus you may exclude all `*.edit` and `*.editor` bundles in the following steps. 

* Follow the steps in the normal Slingshot Documentation (https://palladiosimulator.github.io/Palladio-Documentation-Slingshot/tutorial/) to setup Slingshot and maybe run an (normal) example. 
Pay attention to also import the SPD meta model and the SPD interpreter extension.
  
* clone [Palladio-Analyzer-Slingshot-Extension-Initialisation](https://github.com/PalladioSimulator/Palladio-Analyzer-Slingshot-Extension-Initialisation)
  ```
  git clone git@github.com:PalladioSimulator/Palladio-Analyzer-Slingshot-Extension-Initialisation.git
  ```


### Currently: separated branches, while changes are not yet merged. 
* in (already cloned) repository *Palladio-Analyzer-Slingshot* switch to branch `stateexplorationRequirements`
* in (already cloned) repository *Palladio-Analyzer-Slingshot-Extension-PCM-Core* switch to branch `stateexplorationRequirements`
*Palladio-Analyzer-Slingshot-Extension-SPD-Interpreter* switch to branch `stateexplorationRequirements`
* all other Slingshot repositories (`Cost`, `Monitoring`) remain on `master`. 

+ Import Experiment-Automation bundles. 
  * clone repository [Palladio-Addons-ExperimentAutomation](https://github.com/PalladioSimulator/Palladio-Addons-ExperimentAutomation) and switch to branch `slingshot-impl`:
  ```
  git clone git@github.com:PalladioSimulator/Palladio-Addons-ExperimentAutomation.git
  git checkout slingshot-impl
  ```
  * import the bundle `org.palladiosimulator.experimentautomation` into the workspace of your development Eclipse instance.
  We only need the models, thus we only need this bundle. 

## Running an initialised Slingshot simulation run
Both approaches require **Models and JSON files**.

### Run from within the development Eclipse instance
* **Requires: all bundles described above imported into the workspace**
+ Create an *OSGi Framework* Run Configuration.
  * Or use this one `org.palladiosimulator.analyzer.slingshot.stateexploration.application/launchconfig/headless-exploration-export.launch` 
    * **Beware:** you still need to fix the *Program arguments* (see below)
  * Exclude all `*.edit`, `*.editor` and `*.ui` bundles. 
    * Excluding `*.edit` is not required, but we don't need it.
    * Only exclude `*.ui`. The `*.ui.events` are still needed.   
  * Uncheck *Include optional dependencies [...]* 
  * Click *Add Required Bundles*, to add dependencies
  * Click *Validate Bundles*, just to be on the safe side.
  * Go to tab *Arguments*
  * Add to *Program arguments*: `-application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication /path/to/experiments/file.experiments /path/to/snapshot/file.json /path/to/config/file.json /path/to/output/folder`
  * Remove from *VM arguments*: `-Declipse.ignoreApp=true`
  * (Optional) add to *VM arguments*: `-Dlog4j.configuration=file:///path/to/log4j.properties`
  * Run it.


### Run from Commandline
* **Requires: PalladioBench with Initialisation Extension is already installed**
* Execute
  ```
  ./PalladioBench -data /path/to/workspace/ \
  -application org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application.InitialisedSimulationApplication \
  /path/to/experiments/file.experiments \
  /path/to/snapshot/file.json \
  /path/to/config/file.json \
  /path/to/output/folder \
  -vmargs -Xmx4G -Dlog4j.configuration=file:///path/to/log4j.properties
  ```