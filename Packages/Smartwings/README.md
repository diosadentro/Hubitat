# SmartWings Zigbee Shades README

# Prerequisites

Follow the instructions that came with your shades or visit [Smartwingshome.com](https://www.smartwingshome.com/) to setup shades. This driver does not control limits, speed, remote configuration, etc. so all that must be completed prior to utilizing this driver.

# Installation Options

* **[Recommended]** [Hubitat Package Manager]()
* Raw code https://github.com/diosadentro/hubitat-smartwings-shades

# Product Links

Links to purchase the Smartwings Zigbee Shades.
* [SmartWings Motorized Light Filtering Roller Shades 70% Blackout Safari](https://www.smartwingshome.com/products/smartwings-motorized-light-filtering-roller-shades-70-blackout-safari). The ones I've test this on are both inside and outside mount, all motor on the right, Zigbee Smart Motor option, with and without a valance, standard rollback side.


# Capabilities

* Open
* Close
* Stop
* Get and set position [0-100]
* Get and set level [0-100]
* Battery Level (Currently broken)

## Tested Devices

Has only been directly tested with devices listed below. But may work with other SmartWings shades and blinds. I've added an invert configuration to invert the open and close numbers. This may help if your motor is mounted on the right or left side or if your shade decends from the back or front of the shade. Some community posts in HA suggested this may be a problem.

### Tested Motor Type

* WM25/L-Z

# Google/Alexa Home Integration

* To-Do: I've included the Switch Level capabilities so this device should be able to be treated like a switch to set position. I've found that when using Groups and Scenes, it's best to use set level vs. open and close to increse the likelihood that they all open and close together. You should be able to add a group to Alexa to control the shades.