/**
 *  Eyedro Home Energy Monitor driver for Hubitat Elevation - Power Factor, Watts, Voltage, Amperage
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  ver. 1.0.0 2022-01-22 diosadentro - first version: - reads Power Factor, Watts, Amps, Voltage on a set interval
 *
 */

metadata {
    definition (name: "Eyedro Energy Sensor", namespace: "hubitat", author: "Greg Billings") {
        capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
        capability "Refresh"
        capability "Polling"
        
        attribute "powerFactor", "number"
        attribute "powerFactorOne", "number"
        attribute "powerFactorTwo", "number"
        attribute "voltage", "number"
        attribute "voltageOne", "number"
        attribute "voltageTwo", "number"
        attribute "amps", "number"
        attribute "amperageOne", "number"
        attribute "amperageTwo", "number"
        attribute "watts", "number"
        attribute "wattageOne", "number"
        attribute "wattageTwo", "number"
        
        attribute "powerFactorDisplay", "string"
        attribute "powerFactorOneDisplay", "string"
        attribute "powerFactorTwoDisplay", "string"
        attribute "demandDisplay", "string"
        attribute "voltageDisplay", "string"
        attribute "voltageOneDisplay", "string"
        attribute "voltageTwoDisplay", "string"
        attribute "ampsDisplay", "string"
        attribute "amperageOneDisplay", "string"
        attribute "amperageTwoDisplay", "string"
        attribute "wattsDisplay", "string"
        attribute "wattageOneDisplay", "string"
        attribute "wattageTwoDisplay", "string"
        

        command "reset"
    }

    preferences {
        input name: 'ipAddress', type: 'string', description: '', title: 'IP Address', defaultValue: ''
        input name: 'pullSeconds', type: 'number', description: '', title: 'Pull interval in seconds', defaultVaule: 10
        input name: 'loggingEnabled', type: 'bool', title: 'Enable informational logging', defaultValue: true
    }
}

private IP_ADDRESS() { ipAddress }
private PULL_SECONDS() { pullSeconds }
private LOGGING_ENABLED() { loggingEnabled }

/**
 * Hubitat DTH Lifecycle Functions
 **/
void installed() {
    reset()
    poll()
}

void initialize() {
    reset()
    poll()
}

void updated() {
    reset()
    poll()
}

def logData(data) {
    if(LOGGING_ENABLED()) {
        log.debug(data)
    }
}

def getData() {
    logData("refresh(): ${IP_ADDRESS()}")
    def ParamsGN;
    ParamsGN = [
            uri:  "http://${IP_ADDRESS()}:8080/getdata"
    ]
    logData(ParamsGN[uri])
    asynchttpGet('processData', ParamsGN)
    return
}

def processData(resp, data) {
    logData('Data Result')
    logData(resp.data)

    if(resp.getStatus() < 200 || resp.getStatus() >= 300) {
        log.warn('Calling ' + "getData")
        log.warn(resp.getStatus() + ':' + resp.getErrorMessage())
    } else {
        def json = parseJson(resp.data)
        state.powerFactorOne = json.data[0][0]/1000
        state.voltageOne = json.data[0][1]/100
        state.amperageOne = json.data[0][2]/1000
        state.wattageOne = json.data[0][3]
        
        state.powerFactorTwo = json.data[1][0]/1000
        state.voltageTwo = json.data[1][1]/100
        state.amperageTwo = json.data[1][2]/1000
        state.wattageTwo = json.data[1][3]
        
        state.powerFactor = state.powerFactorOne + state.powerFactorTwo
        state.amps = state.amperageOne + state.amperageTwo
        state.voltage = state.voltageOne + state.voltageTwo
        state.watts = state.wattageOne + state.wattageTwo
        
        resetDisplay()
    }
}

def resetDisplay() {
	logData("resetDisplay()")
	
    sendEvent(name: "demandDisp", value: "Total\n" + state.watts/1000 + "\nkW", unit: "kW")
    sendEvent(name: "powerFactorDisp", value: "Total\n" + state.powerFactor + "\n%", unit: "%")
    sendEvent(name: "powerFactorOneDisp", value: state.powerFactorOne + "\n", unit: "")
    sendEvent(name: "powerFactorTwoDisp", value: state.powerFactorTwo + "\n", unit: "")
    sendEvent(name: "appsDisp", value: "Total\n" + state.amps + "\nA", unit: "A")
    sendEvent(name: "amperageOneDisp", value: state.amperageOne + "\nA", unit: "A")
    sendEvent(name: "amperageTwoDisp", value: state.amperageTwo + "\nA", unit: "A")
    sendEvent(name: "wattsDisp", value: "Total\n" + state.watts + "\nW", unit: "W")
    sendEvent(name: "wattageOneDisp", value: state.wattageOne + "\nW", unit: "W")
    sendEvent(name: "wattageTwoDisp", value: state.wattageTwo + "\nW", unit: "W")
    sendEvent(name: "voltageDisp", value: "Total\n" + state.voltage + "\nV", unit: "V")
    sendEvent(name: "voltageOneDisp", value: state.voltageOne + "\nV", unit: "V")
    sendEvent(name: "voltageTwoDisp", value: state.voltageTwo + "\nV", unit: "V")
}

def reset() {
	logData("reset()")
	
    state.watts = 0
    state.wattageOne = 0
    state.wattageTwo = 0
    state.powerFactor = 0
    state.powerFactorOne = 0
    state.powerFactorTwo = 0
    state.amps = 0
    state.amperageOne = 0
    state.amperageTwo = 0
    state.voltage = 0
    state.voltageOne = 0
    state.voltageTwo = 0
	
    resetDisplay()
}

def poll() {
    logData("poll()")
    getData()
    runIn(PULL_SECONDS(), poll)
}

