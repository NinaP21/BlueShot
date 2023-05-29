##################################################################################################
## 
##  Xilinx, Inc. 2010            www.xilinx.com 
##  Sun 14. May 21:38:55 2023
##  Generated by MIG Version 4.1
##  
##################################################################################################
##  File name :       example_top.sdc
##  Details :     Constraints file
##                    FPGA Family:       SPARTAN7
##                    FPGA Part:         XC7S25CSGA324_PKG
##                    Speedgrade:        -1
##                    Design Entry:      VERILOG
##                    Frequency:         0 MHz
##                    Time Period:       3077 ps
##################################################################################################

##################################################################################################
## Controller 0
## Memory Device: DDR3_SDRAM->Components->MT41K128M16XX-15E
## Data Width: 16
## Time Period: 3077
## Data Mask: 1
##################################################################################################

set_property IO_BUFFER_TYPE NONE [get_ports {ddr3_ck_n[*]} ]
set_property IO_BUFFER_TYPE NONE [get_ports {ddr3_ck_p[*]} ]
          
create_clock -period 10 [get_ports sys_clk_i]
          
#create_clock -period 5 [get_ports clk_ref_i]
          
############## NET - IOSTANDARD ##################



set_property INTERNAL_VREF  0.675 [get_iobanks 34]