# BlueShot
Communication between FPGA and Android over BLE protocol

<img src="https://github.com/NinaP21/BlueShot/blob/master/logo.png" align="right" alt="BlueShot logo" width="150" height="150">

<p>This project has been implemented in the context of my diploma thesis, while studying at Computer Engineering and Informatics Department (CEID) of the University of Patras. It is about an Android application that captures a low-resolution picture and sends it to an FPGA-based system, via Bluetooth Low Energy (BLE) protocol. Then, the embedded system processes the image by applying a Gaussian blurring filter and then sends the new picture back to the Android device, so that the user can compare the two images. The main subject of the project concerns the BLE communication between the FPGA and the smartphone.</p>

## Tools and Hardware needed

* The Android application has been developed in ___Android Studio Chipmunk, 2021.2.1___ and the code is written in Java. The app targets ___Android 13___ and can operate in all SDK levels starting from Android SDK 26.
* The development board is the ___Arty S7-50___, that contains the ___Spartan-7 50___ FPGA and 256 MB DDR3 SDPRAM.
* Bluetooth Low Energy in Arty S7-50 is supported by attaching a ___BLE Pmod___ to a Pmod connector.
* The embedded system is designed using ___Vivado 2018.2___ and ___Xilinx SDK 2018.2___.

## Project Structure
* BlueShot Android application is in folder [Android app](https://github.com/NinaP21/BlueShot/tree/master/Android%20app) that contains all necessary files for the Android Studio project.
* [Vivado project](https://github.com/NinaP21/BlueShot/tree/master/Vivado%20project) folder contains two subfolders:
  * [custom IP core](https://github.com/NinaP21/BlueShot/tree/master/Vivado%20project/custom%20IP%20core), that includes all Verilog files needed to create and package a custom IP core in Vivado. The generated block reads a 324x576 8-bit RGB image and applies a Gaussian blurring filter, using a 3x3 kernel.
  * [img_blurring](https://github.com/NinaP21/BlueShot/tree/master/Vivado%20project/img_blurring), that is the design of the embedded system in Vivado IP Integrator. The embedded system operates using ___MicroBlaze soft processor___ and the custom IP core for image blurring. This folder contains the Vivado design, as well as the software that runs on MicroBlaze. 

## Helpful Github Links
* [image processing core in Verilog](https://github.com/vipinkmenon/SpatialFilter)
* [JPEG to BMP in Java](https://github.com/ultrakain/AndroidBitmapUtil/blob/master/src/com/ultrasonic/android/image/bitmap/util/AndroidBmpUtil.java)

***

<img src="https://github.com/NinaP21/BlueShot/blob/master/system.png" align="center" alt="System connection">
