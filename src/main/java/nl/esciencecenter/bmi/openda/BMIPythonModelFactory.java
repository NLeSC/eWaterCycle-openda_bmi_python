/*
 * Copyright 2014 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.esciencecenter.bmi.openda;

import java.io.File;
import java.util.Arrays;

import nl.esciencecenter.bmi.BMIRaster;

import org.openda.blackbox.interfaces.IModelFactory;
import org.openda.interfaces.IModelInstance;
import org.openda.interfaces.IStochModelFactory.OutputLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMIPythonModelFactory implements IModelFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BMIPythonModelFactory.class);

    private static int nextModelID = 0;

    private static int getNextID() {
        int result = nextModelID;

        nextModelID += 1;

        return result;
    }

    private String pythonExecutable;
    private File bridgeDir;
    private File modelDir;
    private String modelModule;
    private String modelClass;

    private File modelRunRootDir;

    private String configFile;

    private String[] hosts;

    //called by OpenDA
    public BMIPythonModelFactory() {
        //NOTHING
    }

    @Override
    public void initialize(File configRootDir, String[] arguments) {
        System.err.println(Arrays.toString(arguments));
        //configFile = "/home/niels/workspace/eWaterCycle-operational/pcrglobwb_config/setup_30min_niels_laptop.ini";
        //configFile = "/home/niels/workspace/eWaterCycle-operational/pcrglobwb_config/setup_RhineMeuse_30arcmin_3layers_ndrost.ini";
        //pythonExecutable = "/home/niels/bin/python";
        //modelClass = "BmiPCRGlobWB";
        //bridgeDir = new File("/home/niels/workspace/eWaterCycle-openda_bmi_python");
        //modelDir = new File("/home/niels/workspace/PCR-GLOBWB/model");
        //modelModule = "bmiPcrglobwb";
        //modelRunRootDir = new File("/home/niels/Data/operational-output");

        pythonExecutable = arguments[0];
        bridgeDir = new File(arguments[1]);
        modelDir = new File(arguments[2]);
        modelModule = arguments[3];
        modelClass = arguments[4];
        modelRunRootDir = new File(arguments[5]);
        configFile = arguments[6];

        if (configFile == null) {
            throw new RuntimeException("Configfile not specified!");
        }

//        //get list of hosts to start models on from environment
//        String hostString = System.getenv("HOSTS");
        String hostString = arguments[7];

        if (hostString == null || hostString.isEmpty()) {
            hosts = new String[] { "localhost" };
        } else {
            //hosts are separated by spaces
            hosts = hostString.split(" ");
        }

    }

    @Override
    public IModelInstance getInstance(String[] arguments, OutputLevel outputLevel) {
        try {
            //ID of this member
            int instanceID = getNextID();

            //we allocate instances to hosts round-robin
            String host = hosts[instanceID % hosts.length];

            //create a output directory for this member
            String workDir = String.format("work%02d", instanceID);
            File modelWorkDir = new File(modelRunRootDir, workDir);
            modelWorkDir.mkdirs();

            BMIRaster model = LocalPythonThriftBMIRaster.createModel(host, pythonExecutable, bridgeDir, modelDir, modelModule,
                    modelClass, modelWorkDir);

            String instanceConfigFile = String.format("%s.%02d", configFile, instanceID);

            return new BMIRasterModelInstance(model, modelWorkDir, instanceConfigFile);
        } catch (Exception e) {
            LOGGER.error("failed to create instance", e);
            return null;
        }
    }

    @Override
    public void finish() {
        // TODO Auto-generated method stub

    }
}
