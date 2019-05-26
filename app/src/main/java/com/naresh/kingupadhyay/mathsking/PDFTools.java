package com.naresh.kingupadhyay.mathsking;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

import static android.content.ContentValues.TAG;
import static android.net.Uri.*;

public class PDFTools {

    private static final String GOOGLE_DRIVE_PDF_READER_PREFIX = "http://drive.google.com/viewer?url=";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String HTML_MIME_TYPE = "text/html";
    private static Context context;
    private static String pdfUrl;

    /**
     * If a PDF reader is installed, download the PDF file and open it in a reader.
     * Otherwise ask the user if he/she wants to view it in the Google Drive online PDF reader.<br />
     * <br />
     * <b>BEWARE:</b> This method
     * @param context
     * @param pdfUrl
     * @return
     */
    public static void showPDFUrl(final Context context, final String pdfUrl ) {
     //   PDFTools.context = context;
       // PDFTools.pdfUrl = pdfUrl;
        if ( isPDFSupported( context ) ) {
            downloadAndOpenPDF(context, pdfUrl);
        } else {
            askToOpenPDFThroughGoogleDrive( context, pdfUrl );
        }
    }


    public static boolean isDownloadManagerAvailable(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return true;
        }
        return false;
    }
    /**
     * Downloads a PDF with the Android DownloadManager and opens it with an installed PDF reader app.
     * @param context
     * @param pdfUrl
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void downloadAndOpenPDF(final Context context, final String pdfUrl) {
        try{
            // Get filename
            final String filename = pdfUrl.substring( pdfUrl.lastIndexOf( "=" ) + 1 ) + ".pdf";
            // The place where the downloaded PDF file will be put
            final File tempFile = new File( context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS ), filename );
            Toast.makeText(context,filename,Toast.LENGTH_LONG).show();
            if ( tempFile.exists() ) {
                // If we have downloaded the file before, just go ahead and show it.
                Toast.makeText(context,"start",Toast.LENGTH_LONG).show();
                openPDF( context, fromFile( tempFile ) );
                return;
            }

            // Show progress dialog while downloading
            final ProgressDialog progDailog = new ProgressDialog(context);
            progDailog.setTitle("Data Loading....");
            progDailog.setMessage("Please Wait.....");
            progDailog.setCancelable(false);
            progDailog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            progDailog.show();           // Create the download request
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pdfUrl));
            request.setDescription("Some descrition");
            request.setTitle("Some title");
// in order for this if to run, you must use the android 3.2 to compile your app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

// get download service and enqueue file
            final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            /*
            DownloadManager.Request r = new DownloadManager.Request( parse( pdfUrl ) );
            r.setDestinationInExternalFilesDir( context, Environment.DIRECTORY_DOWNLOADS, filename );
            Toast.makeText(context,Environment.DIRECTORY_DOWNLOADS,Toast.LENGTH_LONG).show();
            final DownloadManager dm = (DownloadManager) context.getSystemService( Context.DOWNLOAD_SERVICE );*/
            Toast.makeText(context,"Download Manager",Toast.LENGTH_LONG).show();
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ( !progDailog.isShowing() ) {
                        return;
                    }
                    context.unregisterReceiver( this );

                    progDailog.dismiss();
                    long downloadId = intent.getLongExtra( DownloadManager.EXTRA_DOWNLOAD_ID, -1 );
                    Cursor c = manager.query( new DownloadManager.Query().setFilterById( downloadId ) );

                    if ( c.moveToFirst() ) {
                        int status = c.getInt( c.getColumnIndex( DownloadManager.COLUMN_STATUS ) );
                        if ( status == DownloadManager.STATUS_SUCCESSFUL ) {
                            openPDF( context, fromFile( tempFile ) );
                            Toast.makeText(context,"Downloading start",Toast.LENGTH_LONG).show();
                        }
                    }
                    c.close();
                }
            };
            context.registerReceiver( onComplete, new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE ) );

            // Enqueue the request
            //dm.enqueue( r );
            manager.enqueue(request);

            Toast.makeText(context,"Downloading complete",Toast.LENGTH_LONG).show();
        }catch (Exception ex){
            Toast.makeText(context,ex.getMessage(),Toast.LENGTH_LONG).show();
        }finally {
            Toast.makeText(context,"finally",Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Show a dialog asking the user if he wants to open the PDF through Google Drive
     * @param context
     * @param pdfUrl
     */
    public static void askToOpenPDFThroughGoogleDrive( final Context context, final String pdfUrl ) {
        new AlertDialog.Builder( context )
                .setTitle( R.string.pdf_show_online_dialog_title )
                .setMessage( R.string.pdf_show_online_dialog_question )
                .setNegativeButton( R.string.pdf_show_online_dialog_button_no, null )
                .setPositiveButton( R.string.pdf_show_online_dialog_button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPDFThroughGoogleDrive(context, pdfUrl);
                    }
                })
                .show();
    }

    /**
     * Launches a browser to view the PDF through Google Drive
     * @param context
     * @param pdfUrl
     */
    public static void openPDFThroughGoogleDrive(final Context context, final String pdfUrl) {

        Intent i = new Intent( Intent.ACTION_VIEW );
        i.setDataAndType(parse(GOOGLE_DRIVE_PDF_READER_PREFIX + pdfUrl ), HTML_MIME_TYPE );
        context.startActivity( i );
    }
    /**
     * Open a local PDF file with an installed reader
     * @param context
     * @param localUri
     */
    public static final void openPDF(Context context, Uri localUri ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        i.setDataAndType( localUri, PDF_MIME_TYPE );
        context.startActivity( i );
    }
    /**
     * Checks if any apps are installed that supports reading of PDF files.
     * @param context
     * @return
     */
    public static boolean isPDFSupported( Context context ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        final File tempFile = new File( context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS ), "test.pdf" );
        i.setDataAndType( fromFile( tempFile ), PDF_MIME_TYPE );
        return context.getPackageManager().queryIntentActivities( i, PackageManager.MATCH_DEFAULT_ONLY ).size() > 0;
    }
}

