package com.example.blueshot;

import static com.example.blueshot.BluetoothLeService.CURRENT_MTU_SIZE;
import static com.example.blueshot.BluetoothLeService.recvTime;
import static com.example.blueshot.FpgaActivity.sendingTime;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LogsActivity extends AppCompatActivity {

    Button backButton;
    TextView mtuRow, intervalRow, nrPacketsRow, sendRow, recvRow, totalTimeRow, resolRow, headerRow, pixelRow, imgSizeRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        backButton = findViewById(R.id.backToFpgaActivity2);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mtuRow = findViewById(R.id.mtuRow);
        intervalRow = findViewById(R.id.connectionIntRow);
        nrPacketsRow = findViewById(R.id.packetsRow);
        sendRow = findViewById(R.id.sendRow);
        recvRow = findViewById(R.id.recvRow);
        totalTimeRow = findViewById(R.id.timeRow);
        resolRow = findViewById(R.id.resolRow);
        headerRow = findViewById(R.id.headerRow);
        pixelRow = findViewById(R.id.pixelsRow);
        imgSizeRow = findViewById(R.id.imgRow);

        long totalTime = recvTime + sendingTime;

        nrPacketsRow.setText("up to 6");
        mtuRow.setText(CURRENT_MTU_SIZE + " bytes");
        intervalRow.setText("18 ms");
        sendRow.setText(sendingTime + " seconds" );
        recvRow.setText(recvTime + " seconds");
        totalTimeRow.setText(totalTime + " seconds");
        resolRow.setText("324 x 576");
        headerRow.setText("1078");
        pixelRow.setText("186,624");
        imgSizeRow.setText("187,702 bytes");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_transition_slide_in_right, R.anim.activity_transition_slide_out_left);
    }
}