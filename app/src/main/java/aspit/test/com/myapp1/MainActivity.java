package aspit.test.com.myapp1;


import android.os.Bundle;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dynamsoft.camerasdk.exception.DcsCameraNotAuthorizedException;
import com.dynamsoft.camerasdk.exception.DcsException;
import com.dynamsoft.camerasdk.exception.DcsValueNotValidException;
import com.dynamsoft.camerasdk.exception.DcsValueOutOfRangeException;
import com.dynamsoft.camerasdk.io.DcsPDFEncodeParameter;
import com.dynamsoft.camerasdk.model.DcsDocument;
import com.dynamsoft.camerasdk.model.DcsImage;
import com.dynamsoft.camerasdk.view.DcsVideoView;
import com.dynamsoft.camerasdk.view.DcsVideoViewListener;
import com.dynamsoft.camerasdk.view.DcsView;
import com.dynamsoft.camerasdk.view.DcsViewListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity implements DcsViewListener {

    private DcsView dcsView;
    private TextView tvTitle;
    private TextView tvShow;
    private static final int CAMERA_OK = 10;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    String mCurrentPhotoPath;
    Uri imageURI;
    boolean tapped = false;
    private String pdfName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            DcsView.setLicense(getApplicationContext(), "483AEC5578C7F3EF8AE656DFA929919B5DF7D701");
        } catch (DcsValueNotValidException e) {
            e.printStackTrace();
        }
        tvTitle = findViewById(R.id.tv_title_id);
        tvShow = findViewById(R.id.tv_show_id);
        dcsView = findViewById(R.id.dcsview_id);
        dcsView.setCurrentView(DcsView.DVE_IMAGEGALLERYVIEW);
        dcsView.setListener(this);

        try {
            dcsView.getVideoView().setMode(DcsView.DME_DOCUMENT);
        } catch (DcsValueOutOfRangeException e) {
            e.printStackTrace();
        }

        dcsView.getVideoView().setNextViewAfterCancel(DcsView.DVE_IMAGEGALLERYVIEW);
        dcsView.getVideoView().setNextViewAfterCapture(dcsView.DVE_EDITORVIEW);


        tvShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dcsView.setCurrentView(DcsView.DVE_VIDEOVIEW);
            }
        });
        dcsView.getVideoView().setListener(new DcsVideoViewListener() {


            @Override
            public boolean onPreCapture(DcsVideoView dcsVideoView) {

                return true;
            }

            @Override
            public void onCaptureFailure(DcsVideoView dcsVideoView, DcsException e) {

            }

            @Override
            public void onPostCapture(DcsVideoView dcsVideoView, DcsImage dcsImage) {
                savePicture(dcsImage.getImage()); //<-Save Image
                if (dcsView.getBuffer().getCurrentIndex() > 0) { //<-Delete previous Image
                    dcsView.getBuffer().delete(0);
                }
                int[] array = {0};
                dcsView.getIO().save(array, getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/" + pdfName + ".pdf", new DcsPDFEncodeParameter()); //<-Save as a PDF
            }

            @Override
            public void onCancelTapped(DcsVideoView dcsVideoView) {

            }

            @Override
            public void onCaptureTapped(DcsVideoView dcsVideoView) {

                tapped = true;

            }

            @Override
            public void onDocumentDetected(DcsVideoView dcsVideoView, DcsDocument dcsDocument) {

            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (dcsView.getCurrentView() == DcsView.DVE_VIDEOVIEW) {
            try {
                dcsView.getVideoView().preview();
            } catch (DcsCameraNotAuthorizedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
        if (dcsView.getCurrentView() == DcsView.DVE_VIDEOVIEW) {
            try {
                dcsView.getVideoView().preview();
            } catch (DcsCameraNotAuthorizedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        dcsView.getVideoView().stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dcsView.getVideoView().destroyCamera();
    }

    @Override
    public void onCurrentViewChanged(DcsView dcsView, int lastView, int currentView) {

        if (currentView == DcsView.DVE_IMAGEGALLERYVIEW) {
            tvShow.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvShow.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > 22) {
            try {
                if (ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_OK);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // do nothing
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            DcsView.setLicense(getApplicationContext(), "your license number");
        } catch (DcsValueNotValidException e) {
            e.printStackTrace();
        }
    }

    private void savePicture(Bitmap documentImage) {

        // Create the File where the photo should go
        File photoFile = null;

        try {
            photoFile = createImageFile(documentImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            imageURI = FileProvider.getUriForFile(this,
                    "aspit.test.com.myapp1",
                    photoFile);


        }

    }

    private File createImageFile(Bitmap documentImage) throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_TEST_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Convert bitmap to Image
        mCurrentPhotoPath = image.getAbsolutePath();
        File file = new File(mCurrentPhotoPath);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        documentImage.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();
        pdfName = file.getName();

        return image;
    }

}
