# BlueShot
Communication between FPGA and Android over BLE protocol

<img src="https://github.com/NinaP21/BlueShot/blob/master/logo.png" align="right" alt="BlueShot logo" width="150" height="150">

<p>This project has been implemented in the context of my diploma thesis, while studying at Computer Engineering and Informatics Department (CEID) of the University of Patras. It is about an Android application that captures a low-resolution picture and sends it to an FPGA-based system, via Bluetooth Low Energy (BLE) protocol. Then, the embedded system processes the image by applying a Gaussian blurring filter and then sends the new picture back to the Android device, so that the user can compare the two images. The main subject of the project concerns the BLE communication between the FPGA and the smartphone.</p>

## Tools and Hardware needed

* The Android application has been developped in ___Android Studio Chipmunk, 2021.2.1___ and the code is written in Java. The app targets ___Android 13___ and can operate in all SDK levels starting from Android SDK 26.
* The development board is the ___Arty S7-50___, that contains the ___Spartan-7 50___ FPGA and 256 MB DDR3 SDPRAM.
* Bluetooth Low Energy in Arty S7-50 is supported by attaching a ___BLE Pmod___ to a Pmod connector.
* The embedded system is designed using ___Vivado 2018.2___ and ___Xilinx SDK 2018.2___.

## Project Structure


***
<img src="https://github.com/NinaP21/BlueShot/blob/master/system.png" align="center" alt="System connection">
