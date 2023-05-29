-- Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.
-- --------------------------------------------------------------------------------
-- Tool Version: Vivado v.2018.2 (win64) Build 2258646 Thu Jun 14 20:03:12 MDT 2018
-- Date        : Sun Mar 12 23:31:22 2023
-- Host        : LAPTOP-7D6BID1H running 64-bit major release  (build 9200)
-- Command     : write_vhdl -force -mode synth_stub
--               C:/Users/ninap/Desktop/vivado_projects/img_blurring/img_blurring.srcs/sources_1/bd/img_blurring/ip/img_blurring_img_process_core_0_0/img_blurring_img_process_core_0_0_stub.vhdl
-- Design      : img_blurring_img_process_core_0_0
-- Purpose     : Stub declaration of top-level module interface
-- Device      : xc7s50csga324-1
-- --------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity img_blurring_img_process_core_0_0 is
  Port ( 
    axi_clock : in STD_LOGIC;
    axi_resetn : in STD_LOGIC;
    i_data_valid : in STD_LOGIC;
    i_data : in STD_LOGIC_VECTOR ( 31 downto 0 );
    o_data_ready : out STD_LOGIC;
    o_data_valid : out STD_LOGIC;
    o_data : out STD_LOGIC_VECTOR ( 31 downto 0 );
    i_data_ready : in STD_LOGIC;
    intr : out STD_LOGIC
  );

end img_blurring_img_process_core_0_0;

architecture stub of img_blurring_img_process_core_0_0 is
attribute syn_black_box : boolean;
attribute black_box_pad_pin : string;
attribute syn_black_box of stub : architecture is true;
attribute black_box_pad_pin of stub : architecture is "axi_clock,axi_resetn,i_data_valid,i_data[31:0],o_data_ready,o_data_valid,o_data[31:0],i_data_ready,intr";
attribute X_CORE_INFO : string;
attribute X_CORE_INFO of stub : architecture is "img_process_core,Vivado 2018.2";
begin
end;
