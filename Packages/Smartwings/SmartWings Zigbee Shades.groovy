/**
 *  Copyright 2022
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 * This DTH is coded based on AXIS Gear ST DTH found here:
 * https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/axis/axis-gear-st.src/axis-gear-st.groovy
 *
 * VERSION HISTORY
 *                                  
 * 1.0.0 (2022-10-10) [Greg Billings] - Initial Support. Mostly works minus battery percentage
 */

import groovy.json.JsonOutput

metadata {
	definition (name: "SmartWings Motor Shades", namespace: "smartwings", author: "Gregory Billings") {
		capability "WindowShade"
        capability "SwitchLevel"
		capability "Battery"
		capability "Refresh"
		capability "Configuration"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0102", outClusters: "0003,0019", manufacturer: "Smartwings", model: "WM25/L-Z"
	}

	preferences {
		input("debugEnable", "bool", title: "Enable debug logging?")
        input("invertLevel", "bool", title: "Invert Level?")
        input("appleHomeSupport", "bool", title: "Apple Home Support? Override SetPosition")
	}
}

//Declare Clusters
private getCLUSTER_POWER() {0x0001}
private getPOWER_ATTR_BATTERY() {0x0021}
private getCLUSTER_WINDOWCOVERING() {0x0102}
private getWINDOWCOVERING_ATTR_LIFTPERCENTAGE() {0x0008}
private getWINDOWCOVERING_CMD_STOP() {0x02}
private getWINDOWCOVERING_CMD_GOTOLIFTPERCENTAGE() {0x05}

def getLastShadeLevel() {
    def value = 0;
    if(device.currentState("shadeLevel"))
        value = device.curentState("shadeLevel")
    else if(device.currentState("level"))
        value = device.curentState("level")
            
    if(invertLevel) value = 100 - value
    return value;
}

def stopPositionChange() {
	if (debugEnable) log.info "stopPositionChange()"
    sendEvent(name: "windowShade", value: "stopping")
    return zigbee.command(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_CMD_STOP)
}

def setShadeLevel(value) {
	if (debugEnable) log.info "setShadeLevel ($value)"
    if(invertLevel) value = 100 - value

	sendEvent(name:"level", value: value, unit:"%", displayed: false)
	sendEvent(name:"shadeLevel", value: value, unit:"%")
    sendEvent(name:"position", value: value, displayed: false)
    log.info "Setting shade level value to $value"

    return zigbee.command(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_CMD_GOTOLIFTPERCENTAGE, zigbee.convertToHexString(value.toInteger(),2))
}

//Send Command through setShadeLevel()
def open() {
    def value = 0
	if (debugEnable) log.info "open()"
	sendEvent(name: "windowShade", value: "opening")
    return setShadeLevel(value)
}

//Send Command through setShadeLevel()
def close() {
    def value = 100
	if (debugEnable) log.info "close()"
	sendEvent(name: "windowShade", value: "closing")
    return setShadeLevel(value)
}

// Send Command through setShadeLevel()
def setLevel(value, duration) {
    if (debugEnable) log.info "setLevel($value)"
    return setShadeLevel(value)
}

// Send Command through setShadeLevel()
def on() {
    if (debugEnable) log.info "on()"
    return open()
}

// Send Command through setShadeLevel()
def off() {
    if (debugEnable) log.info "off()"
    return close()
}

// Send Command through setShadeLevel()
def setPosition(value) {
    if(appleHomeSupport) value = 100 - value
    if (debugEnable) log.info "setPosition($value)"
    return setShadeLevel(value)
}

def startPositionChange(value) {
    if(debugEnable) log.info "startPositionChange($value)"  
    if(value == "close")
        close()
    else
        open()
}

// Set the shade position text for tiles and other status indicators
def setWindowShade(value) {
	if (value > 0 && value < 99) {
		sendEvent(name: "windowShade", value: "partially open")
	}
	else if (value == 0) {
        if(invertLevel)
		    sendEvent(name: "windowShade", value: "closed")
        else
            sendEvent(name: "windowShade", value: "open")
	}
	else {
        if(invertLevel)
		    sendEvent(name: "windowShade", value: "open")
        else
            sendEvent(name: "windowShade", value: "closed")
	}
}

//Refresh command
def refresh() {
	if (debugEnable) log.debug "parse() refresh"
    
	def cmds_refresh = null
	cmds_refresh =  zigbee.readAttribute(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_ATTR_LIFTPERCENTAGE) +
					zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY)

	if (debugEnable) log.info "refresh() --- cmds: $cmds_refresh"

	return cmds_refresh
}

//configure reporting
def configure() {
	sendEvent(name: "windowShade", value: "unknown")
    
	if (debugEnable) log.debug "Configuring Reporting and Bindings."
    
	sendEvent(name: "checkInterval", value: (2 * 60 * 60 + 10 * 60), displayed: true, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "stop"]), displayed: false)

	def attrs_refresh = zigbee.readAttribute(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_ATTR_LIFTPERCENTAGE) +
						zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY)

	def cmds = zigbee.configureReporting(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_ATTR_LIFTPERCENTAGE, 0x20, 1, 3600, 0x00) +
			   zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY, 0x20, 1, 3600, 0x01)

	if (debugEnable) log.info "configure() --- cmds: $cmds"
    
	return attrs_refresh + cmds
}

def parse(String description) {
	if (debugEnable) log.trace "parse() --- description: $description"
	Map map = [:]

	if ((description?.startsWith('read attr -')) || (description?.startsWith('attr report -'))) {
		map = parseReportAttributeMessage(description)
		def result = map ? createEvent(map) : null

		if (map.name == "level") {
            def homeSupportValue = map.value
            if(appleHomeSupport) homeSupportValue = 100 - homeSupportValue
			result = [result, createEvent([name: "shadeLevel", value: map.value, unit: map.unit]), createEvent([name: "position", value: homeSupportValue, unit: map.unit])]
		}

		if (debugEnable) log.debug "parse() --- returned: $result"
		return result
	}
}

private Map parseReportAttributeMessage(String description) {
	Map descMap = zigbee.parseDescriptionAsMap(description)
	Map resultMap = [:]
	if (descMap.clusterInt == CLUSTER_POWER && descMap.attrInt == POWER_ATTR_BATTERY) {
		def batteryValue = Math.round(Integer.parseInt(descMap.value))
        
		if (debugEnable) log.debug "parseDescriptionAsMap() --- Battery: $batteryValue"
        
        resultMap.name = "battery"
        resultMap.unit = "%"
        resultMap.value = batteryValue
        resultMap.displayed = true
	}
	else if (descMap.clusterInt == CLUSTER_WINDOWCOVERING && descMap.attrInt == WINDOWCOVERING_ATTR_LIFTPERCENTAGE) {
		def levelValue = Integer.parseInt(descMap.value, 16)
        if(invertLevel) levelValue = 100 - levelValue
        
        if (debugEnable) log.debug "parseDescriptionAsMap() --- Level: $levelValue"
		
        state.level = levelValue
        resultMap.value = levelValue
        resultMap.name = "level"
		resultMap.unit = "%"
		resultMap.displayed = true
        setWindowShade(levelValue)
    }
	else {
		if (debugEnable) log.debug "parseReportAttributeMessage() --- ignoring attribute"
	}
	return resultMap
}
