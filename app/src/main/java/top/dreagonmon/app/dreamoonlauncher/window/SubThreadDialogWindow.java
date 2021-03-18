package top.dreagonmon.app.dreamoonlauncher.window;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import androidx.annotation.NonNull;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.R;

public class SubThreadDialogWindow {
    private final MainActivity viewContext;
    private final AlertDialog.Builder alertDialog;
    private final AlertDialog.Builder confirmDialog;
    private final AlertDialog.Builder promptDialog;
    private final EditText promptResult;
    private boolean confirmResult;
    private AlertDialog alertDialogInstance;
    private AlertDialog confirmDialogInstance;
    private AlertDialog promptDialogInstance;
    public SubThreadDialogWindow(MainActivity viewContext){
        this.viewContext = viewContext;
        alertDialog = new AlertDialog.Builder(viewContext);
        alertDialog.setTitle(R.string.dialog_alert_title);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(R.string.dialog_confirm,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancelAlertDialog();
            }
        });
        confirmDialog = new AlertDialog.Builder(viewContext);
        confirmDialog.setTitle(R.string.dialog_confirm_title);
        confirmDialog.setCancelable(false);
        confirmDialog.setPositiveButton(R.string.dialog_confirm,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                confirmResult = true;
                cancelConfirmDialog();
            }
        });
        confirmDialog.setNegativeButton(R.string.dialog_cancel,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                confirmResult = false;
                cancelConfirmDialog();
            }
        });
        promptDialog = new AlertDialog.Builder(viewContext);
        promptDialog.setTitle(R.string.dialog_prompt_title);
        promptDialog.setCancelable(false);
        promptResult = new EditText(viewContext);
        promptDialog.setView(promptResult);
        promptDialog.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancelPromptDialog();
            }
        });
    }
    private void cancelAlertDialog(){
        synchronized (alertDialog) {
            if (alertDialogInstance != null) {
                alertDialogInstance.cancel();
                alertDialogInstance = null;
                alertDialog.notify();
            }
        }
    }
    private void cancelConfirmDialog(){
        synchronized (confirmDialog) {
            if (confirmDialogInstance != null) {
                confirmDialogInstance.cancel();
                confirmDialogInstance = null;
                confirmDialog.notify();
            }
        }
    }
    private void cancelPromptDialog(){
        synchronized (promptDialog) {
            if (promptDialogInstance != null) {
                promptDialogInstance.cancel();
                promptDialogInstance = null;
                promptDialog.notify();
            }
        }
    }
    public void alert(String msg){
        synchronized (alertDialog) {
            alertDialog.setMessage(msg);
            // Run On UI Thread
            viewContext.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    alertDialogInstance = alertDialog.show();
                }
            });
            try {
                alertDialog.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            viewContext.enterFullScreen();
        }
    }
    public boolean confirm(String msg){
        synchronized (confirmDialog) {
            confirmDialog.setMessage(msg);
            // Run On UI Thread
            viewContext.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    confirmDialogInstance = confirmDialog.show();
                }
            });
            try {
                confirmDialog.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            viewContext.enterFullScreen();
            return confirmResult;
        }
    }
    public String prompt(String msg){
        synchronized (promptDialog) {
            promptDialog.setMessage(msg);
            promptResult.setText("");
            // Run On UI Thread
            viewContext.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    promptDialogInstance = promptDialog.show();
                }
            });
            try {
                promptDialog.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            viewContext.enterFullScreen();
            return promptResult.getText().toString();
        }
    }
}
