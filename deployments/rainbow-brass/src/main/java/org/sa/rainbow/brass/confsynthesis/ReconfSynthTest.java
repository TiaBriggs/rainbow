package org.sa.rainbow.brass.confsynthesis;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.sa.rainbow.brass.model.p2_cp3.robot.CP3RobotState.Sensors;

import com.google.common.base.Objects;

public class ReconfSynthTest extends ReconfSynth {

	
	
	
	/**
	 * Mock-up methods that mimic the output of different models
	 * only used for testing purposes (extraction of string with initialization 
	 * constants for components, sensors, and parameters of the architecture)
	 * This string is supplied as starting architectural state to compute reconfigurations.
	 * @return
	 */
	
	public Collection<String> getActiveComponents(){
		LinkedList<String> res = new LinkedList<String>();
		res.add("amcl");
		res.add("laserScan_nodelet");
		return res;
	}
	
	public Collection<String> getFailedComponents(){
		LinkedList<String> res = new LinkedList<String>();
		res.add("mrpt");
		return res;
	}
	
	public Collection<String> getInactiveComponents(){
		LinkedList<String> res = new LinkedList<String>();
		res.add("marker_pose_publisher");
		res.add("aruco_marker_publisher_front");
		res.add("aruco_marker_publisher_back");
		return res;
	}
	
	
	public EnumSet <Sensors> getAvailableSensors(){
		EnumSet<Sensors> available = EnumSet.allOf(Sensors.class);
		available.remove(Sensors.KINECT);
		return available;
	}
	
	public EnumSet<Sensors> getFailedSensors(){
		EnumSet<Sensors> failed = EnumSet.allOf(Sensors.class);
		failed.remove(Sensors.CAMERA);
		failed.remove(Sensors.LIDAR);
		failed.remove(Sensors.HEADLAMP);
		return failed;
	}
	
	
	public boolean isKinectOn() throws Exception {
		return true;
	}


	public boolean isLidarOn() throws Exception {
		return true;
	}


	public boolean isBackCameraOn() throws Exception {
		return true;
	}


	public boolean isHeadlampOn() throws Exception {
		return false;
	}
	
	
	/**
	 * Generates initialization string for constants to compute reconfigurations
	 * @return
	 */
	
	public String getCurrentConfigurationInitConstants(){
		String res="";
		
		int i=0;
		for(String c : getInactiveComponents()){
			if (!Objects.equal(null, COMPONENT_NAMES.get(c))){
				if (i>0)
					res+=",";
				res+=COMPONENT_NAMES.get(c)+"_INIT="+ConfigurationSynthesizer.m_component_modes.get("DISABLED");
				i++;
			}
		}
		
		for(String c : getActiveComponents()){
			if (!Objects.equal(null, COMPONENT_NAMES.get(c))){
				res+=",";
				res+=COMPONENT_NAMES.get(c)+"_INIT="+ConfigurationSynthesizer.m_component_modes.get("ENABLED");
			}
		}
		
		for(String c : getFailedComponents()){
			if (!Objects.equal(null, COMPONENT_NAMES.get(c))){
				res+=",";
				res+=COMPONENT_NAMES.get(c)+"_INIT="+ConfigurationSynthesizer.m_component_modes.get("OFFLINE");
			}
		}

		for (Sensors s: getAvailableSensors()){
			if (!Objects.equal(null, SENSOR_NAMES.get(s))){
				boolean sensorOn = false;
				switch (s){
				case KINECT:
					try{
						sensorOn = isKinectOn();
					} catch(Exception e){
						System.out.println("Illegal state exception determining if Sensor is On.");
					}
					break;
				case CAMERA:
					try{
						sensorOn = isBackCameraOn();
					} catch(Exception e){
						System.out.println("Illegal state exception determining if Sensor is On.");
					}
					break;
				case LIDAR:
					try{
						sensorOn = isLidarOn();
					} catch(Exception e){
						System.out.println("Illegal state exception determining if Sensor is On.");
					}
					break;
				case HEADLAMP:
					try{
						sensorOn = isHeadlampOn();
					} catch(Exception e){
						System.out.println("Illegal state exception determining if Sensor is On.");
					}
					break;

				}
				String compModeStr = ConfigurationSynthesizer.m_component_modes.get("DISABLED");
				if (sensorOn)
					compModeStr = ConfigurationSynthesizer.m_component_modes.get("ENABLED");
				res+=",";
				res+=SENSOR_NAMES.get(s)+"_INIT="+compModeStr;
			}
		}
		
		for (Sensors s: getFailedSensors()){
			res+=",";
			res+=SENSOR_NAMES.get(s)+"_INIT="+ConfigurationSynthesizer.m_component_modes.get("OFFLINE");
		}
		
		res+=",fullSpeedSetting0_INIT="+ConfigurationSynthesizer.m_component_modes.get("DISABLED"); // This has to be changed!! Hardwired for the time being.
		res+=",halfSpeedSetting0_INIT="+ConfigurationSynthesizer.m_component_modes.get("ENABLED");
		
		// Rework to this:
		// STOPPED = speed < 0.05ms, SAFE = 0.05 <= speed <= 0.25ms, SLOW = 0.25 < speed < 0.35, FULL otherwise.
		
		return res;		
	}
	
	
	/** 
	 * Test starts here
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		ReconfSynthTest st = new ReconfSynthTest();
		String currentConfStr = st.getCurrentConfigurationInitConstants();
		System.out.println(currentConfStr);
		ConfigurationSynthesizer cs = new ConfigurationSynthesizer();
		cs.generateConfigurations();
		cs.addConfigurationInstances();
		cs.generateBaseModel();
		cs.generateConfigurationPreds();
		System.out.println("Global instance space: "+cs.m_allinstances.toString());
		//System.out.println (cs.generateReconfiguration(currentConfStr, "sol_0"));
		
		
		HashMap<String, List<String>> legalReconfigurations = cs.getLegalReconfigurationsFrom(currentConfStr);
		
		for (String conf: legalReconfigurations.keySet()){
			System.out.println(conf + "->" + legalReconfigurations.get(conf).toString());
		}
		
		
	}
	
}
