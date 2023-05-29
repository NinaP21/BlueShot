`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 08.03.2023 23:53:15
// Design Name: 
// Module Name: processing_control
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////

module processing_control
#(parameter DATA_WIDTH   = 32,
  parameter IMAGE_WIDTH  = 324)
(
    input                  i_clock,
    input                  i_resetn,
    input [DATA_WIDTH-1:0] i_pixel,
    input                  i_pixel_valid,
    output reg [71:0]      o_pixel,
    output                 o_pixel_valid,
    output reg             intr
);
    
    reg [9:0]  i_pixel_line_counter;
    reg [9:0]  read_pixel_counter;
    reg [19:0] total_line_buffer_pixels;
    reg [1:0]  pointer_write_line_buffer;
    reg [1:0]  pointer_read_line_buffer;
    reg [3:0]  lb_i_pixel_valid;
    reg [3:0]  lb_i_read_three_pixels;
    reg [9:0]  total_lines_parsed;
    reg [1:0]  state;
    
    wire [23:0] lb0_o_data;
    wire [23:0] lb1_o_data;
    wire [23:0] lb2_o_data;
    wire [23:0] lb3_o_data;
  
    reg read_line_buffer, first_set_delay;
    
    localparam FILLING_LINE_BUFFERS   = 2'b00,
               READ_FROM_LINE_BUFFERS = 2'b10;
               
    // The module output is valid whenever the line buffers
    // are ready to drive data to the convolution module.
    assign o_pixel_valid = read_line_buffer;
    
    
    // counter that shows how many bytes are stored currently in the three line buffers in use
    // If we are only filling the line buffers and not reading from them,
    // then this counter gets incremented by 1.
    // If we are only reading from the line buffers and not filling them with bytes,
    // then this counter gets decremented by 1.
    // If we are both filling the line buffers and reading from them,
    // then this counter gets incremented and decremented by 1, simultaneously
    // so its value stays the same.
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
        begin
            total_line_buffer_pixels <= 'd0;
        end
        else if (i_pixel_valid & !read_line_buffer)
        begin
            total_line_buffer_pixels <= total_line_buffer_pixels + 'd4;
        end
        else if (!i_pixel_valid & read_line_buffer)
        begin
            total_line_buffer_pixels <= total_line_buffer_pixels - 'd1;
        end
        else if (i_pixel_valid & read_line_buffer)
        begin
            total_line_buffer_pixels <= total_line_buffer_pixels + 'd3;
        end
    end
    
    // State machine that will control the read_line_buffer signal.
    // When there are three line buffers filled with bytes, then 
    // we can drive data to the convolution module.
    // When the convolution module has read a whole image line (read_pixel_counter),
    // then we go back to FILLING_LINE_BUFFERS state to check whether we still 
    // have the needed pixel bytes inside the line buffers.
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
        begin
            first_set_delay    <= 'b1;
            state              <= FILLING_LINE_BUFFERS;
            read_line_buffer   <= 1'b0;
            intr               <= 'b0;
            total_lines_parsed <= 'd1;
        end
        else
        begin
            case (state)
            FILLING_LINE_BUFFERS:
            begin
                intr <= 'b0;
                if (total_line_buffer_pixels >= 3*IMAGE_WIDTH)
                begin
                    read_line_buffer <= 1'b1;
                    state            <= READ_FROM_LINE_BUFFERS;
                end  
            end
            
            READ_FROM_LINE_BUFFERS:
            begin
            
                if (read_pixel_counter == IMAGE_WIDTH-1)
                begin
                    first_set_delay  <= 'b0;
                    intr             <= 'b1;
                    read_line_buffer <= 1'b0;
                    state            <= FILLING_LINE_BUFFERS;
                end
            end

            endcase
        end
    end
    
       
    // Counter for pixels that are stored in the current line buffer.
    // In each line buffer, we store exactly 324 pixels so whenever one line buffer
    // gets full, the pixel counter gets reset.
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
        begin
            i_pixel_line_counter <= 'd0;
        end
        else if ( i_pixel_valid )
        begin
           if (i_pixel_line_counter == (IMAGE_WIDTH - 'd4))
           begin
                i_pixel_line_counter <= 'd0;
           end
           else
                i_pixel_line_counter <= i_pixel_line_counter + 'd4;
        end
    end
    
    // pointer that indicates which line buffer can be used to store
    // the incoming pixels
    // The line buffer pointer gets updated whenever the control module
    // reads a valid pixel that fills the current line buffer up with 324 bytes.
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
        begin
            pointer_write_line_buffer <= 0;
        end
        else if (( i_pixel_line_counter == (IMAGE_WIDTH - 'd4) & i_pixel_valid ))
        begin
            pointer_write_line_buffer <= pointer_write_line_buffer + 'd1;
        end
    end
    
    
    // input valid signals for each line buffer.
    // Every time only one line buffer can accept pixel bytes.
    always @(*)
    begin
        lb_i_pixel_valid = 4'b0;
        lb_i_pixel_valid[pointer_write_line_buffer] = i_pixel_valid;
    end
    
    // counter of line pixels that have been read (and processed) by the convolution module
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
            read_pixel_counter <= 9'b0;
        else if (read_line_buffer)
        begin
            if (read_pixel_counter == (IMAGE_WIDTH - 'd1))
                read_pixel_counter <= 'b0; 
            else
                read_pixel_counter <= read_pixel_counter + 1;
        end
    end
    
    // pointer to the first of the three line buffers that participate in convolution 
    // change whenever we have completed processing an image line -> 324 bytes.
    // Specifically, the two of them stay the same and the third one changes.
    always @(posedge i_clock)
    begin
        if ( !i_resetn )
        begin
            pointer_read_line_buffer <= 0;
        end
        else if (read_pixel_counter == (IMAGE_WIDTH - 'd1) & read_line_buffer)
        begin
            pointer_read_line_buffer <= pointer_read_line_buffer + 'd1;
        end
    end
    
    // combinational circuit that defines the pixels that will participate in convolution
    always @(*)
    begin
        case (pointer_read_line_buffer)
            0:
            begin
                o_pixel <= {lb2_o_data, lb1_o_data, lb0_o_data};
            end
            
            1:
            begin
                o_pixel <= {lb3_o_data, lb2_o_data, lb1_o_data};
            end
            
            2:
            begin
                o_pixel <= {lb0_o_data, lb3_o_data, lb2_o_data};
            end
            
            3:
            begin
                o_pixel <= {lb1_o_data, lb0_o_data, lb3_o_data};
            end
        endcase
    end
    
    // combinational circuit that defines the three line buffers 
    // that will participate in convolution
    always @(*)
    begin
        case (pointer_read_line_buffer)
            0:
            begin
                lb_i_read_three_pixels[0] <= read_line_buffer;
                lb_i_read_three_pixels[1] <= read_line_buffer;
                lb_i_read_three_pixels[2] <= read_line_buffer;
                lb_i_read_three_pixels[3] <= 0;
            end
            
            1:
            begin
                lb_i_read_three_pixels[0] <= 0;
                lb_i_read_three_pixels[1] <= read_line_buffer;
                lb_i_read_three_pixels[2] <= read_line_buffer;
                lb_i_read_three_pixels[3] <= read_line_buffer;
            end
            
            2:
            begin
                lb_i_read_three_pixels[0] <= read_line_buffer;
                lb_i_read_three_pixels[1] <= 0;
                lb_i_read_three_pixels[2] <= read_line_buffer;
                lb_i_read_three_pixels[3] <= read_line_buffer;
            end
            
            3:
            begin
                lb_i_read_three_pixels[0] <= read_line_buffer;
                lb_i_read_three_pixels[1] <= read_line_buffer;
                lb_i_read_three_pixels[2] <= 0;
                lb_i_read_three_pixels[3] <= read_line_buffer;
            end
        endcase
    end
    
    line_buffer #(.DATA_WIDTH(DATA_WIDTH),
                  .IMAGE_WIDTH(IMAGE_WIDTH))
    lb0(
        .i_clock(i_clock),
        .i_resetn(i_resetn),
        .i_pixel(i_pixel),
        .i_pixel_valid(lb_i_pixel_valid[0]),
        .o_three_pixels(lb0_o_data),
        .i_read_three_pixels(lb_i_read_three_pixels[0])
    );
    
    line_buffer #(.DATA_WIDTH(DATA_WIDTH),
                  .IMAGE_WIDTH(IMAGE_WIDTH)) 
    lb1(
        .i_clock(i_clock),
        .i_resetn(i_resetn),
        .i_pixel(i_pixel),
        .i_pixel_valid(lb_i_pixel_valid[1]),
        .o_three_pixels(lb1_o_data),
        .i_read_three_pixels(lb_i_read_three_pixels[1])
    );
        
        
    line_buffer #(.DATA_WIDTH(DATA_WIDTH),
                  .IMAGE_WIDTH(IMAGE_WIDTH)) 
    lb2(
        .i_clock(i_clock),
        .i_resetn(i_resetn),
        .i_pixel(i_pixel),
        .i_pixel_valid(lb_i_pixel_valid[2]),
        .o_three_pixels(lb2_o_data),
        .i_read_three_pixels(lb_i_read_three_pixels[2])
    );
    
    line_buffer #(.DATA_WIDTH(DATA_WIDTH),
                  .IMAGE_WIDTH(IMAGE_WIDTH)) 
    lb3(
        .i_clock(i_clock),
        .i_resetn(i_resetn),
        .i_pixel(i_pixel),
        .i_pixel_valid(lb_i_pixel_valid[3]),
        .o_three_pixels(lb3_o_data),
        .i_read_three_pixels(lb_i_read_three_pixels[3])
    );
    
endmodule