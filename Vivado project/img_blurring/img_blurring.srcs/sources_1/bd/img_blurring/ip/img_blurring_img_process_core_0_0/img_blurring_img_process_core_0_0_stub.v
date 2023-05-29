// Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.
// --------------------------------------------------------------------------------
// Tool Version: Vivado v.2018.2 (win64) Build 2258646 Thu Jun 14 20:03:12 MDT 2018
// Date        : Sun Mar 12 23:31:22 2023
// Host        : LAPTOP-7D6BID1H running 64-bit major release  (build 9200)
// Command     : write_verilog -force -mode synth_stub
//               C:/Users/ninap/Desktop/vivado_projects/img_blurring/img_blurring.srcs/sources_1/bd/img_blurring/ip/img_blurring_img_process_core_0_0/img_blurring_img_process_core_0_0_stub.v
// Design      : img_blurring_img_process_core_0_0
// Purpose     : Stub declaration of top-level module interface
// Device      : xc7s50csga324-1
// --------------------------------------------------------------------------------

// This empty module with port declaration file causes synthesis tools to infer a black box for IP.
// The synthesis directives are for Synopsys Synplify support to prevent IO buffer insertion.
// Please paste the declaration into a Verilog source file or add the file as an additional source.
(* X_CORE_INFO = "img_process_core,Vivado 2018.2" *)
module img_blurring_img_process_core_0_0(axi_clock, axi_resetn, i_data_valid, i_data, 
  o_data_ready, o_data_valid, o_data, i_data_ready, intr)
/* synthesis syn_black_box black_box_pad_pin="axi_clock,axi_resetn,i_data_valid,i_data[31:0],o_data_ready,o_data_valid,o_data[31:0],i_data_ready,intr" */;
  input axi_clock;
  input axi_resetn;
  input i_data_valid;
  input [31:0]i_data;
  output o_data_ready;
  output o_data_valid;
  output [31:0]o_data;
  input i_data_ready;
  output intr;
endmodule
