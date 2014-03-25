package org.commcare.dalvik.activities;

import org.commcare.android.framework.CommCareActivity;
import org.commcare.android.framework.ManagedUi;
import org.commcare.android.framework.UiElement;
import org.commcare.android.javarosa.AndroidLogger;
import org.commcare.android.models.notifications.NotificationMessageFactory;
import org.commcare.android.models.notifications.NotificationMessageFactory.StockMessages;
import org.commcare.android.tasks.ConnectionDiagTask;
import org.commcare.android.tasks.DumpTask;
import org.commcare.android.tasks.ExceptionReportTask;
import org.commcare.android.tasks.SendTask;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author srengesh
 *
 */

@ManagedUi(R.layout.connection_diag)
public class CommCareConnectionDiagActivity extends CommCareActivity<CommCareConnectionDiagActivity> {
	@UiElement(R.id.screen_bulk_image1)
	ImageView banner;
	
	@UiElement(value = R.id.connection_test_prompt, locale="connection.test.prompt")
	TextView connectionPrompt;
	
	@UiElement(value = R.id.run_connection_test, locale="connection.test.run")
	Button btnRunTest;
	
	@UiElement(value = R.id.output_message, locale="connection.test.messages")
	TextView txtInteractiveMessages;
	
	@UiElement(value = R.id.settings_button, locale="connection.test.access.settings")
	Button settingsButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		btnRunTest.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{

				ConnectionDiagTask<CommCareConnectionDiagActivity> mConnectionDiagTask = 
				new ConnectionDiagTask<CommCareConnectionDiagActivity>(getApplicationContext(), CommCareApplication._().getCurrentApp().getCommCarePlatform())		
				{	
					@Override
					protected void deliverResult(CommCareConnectionDiagActivity receiver, String result) 
					{
						txtInteractiveMessages.setText(result);
						txtInteractiveMessages.setVisibility(View.VISIBLE);
						if(result.equals(Localization.get("connection.task.internet.fail"))||
							result.equals(Localization.get("connection.task.remote.ping.fail")))
						{
							settingsButton.setOnClickListener( new OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
								}
							});
							settingsButton.setVisibility(View.VISIBLE);
						}
						else
						{
							settingsButton.setVisibility(View.INVISIBLE);
						}
					}

					@Override
					protected void deliverUpdate(CommCareConnectionDiagActivity receiver, String... update) 
					{
						txtInteractiveMessages.setText((Localization.get("connection.test.update.message")));
					}
					
					@Override
					protected void deliverError(CommCareConnectionDiagActivity receiver, Exception e)
					{
						txtInteractiveMessages.setText(Localization.get("connection.test.error.message"));
						receiver.TransplantStyle(txtInteractiveMessages, R.layout.template_text_notification_problem);
					}
				};
				
				mConnectionDiagTask.connect(CommCareConnectionDiagActivity.this);
				mConnectionDiagTask.execute();				
			}
		});
	}
	
	private AlertDialog newDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle((Localization.get("connection.test.run.title")));
		builder.setMessage(Localization.get("connection.test.now.running"))
		.setCancelable(false)
		.setPositiveButton(Localization.get("connection.test.button.message"), new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dialog.dismiss();
				dialog.cancel();
			}
		});	
		
		AlertDialog dialog = builder.create();
		return dialog;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) 
	{
		if(id == ConnectionDiagTask.CONNECTION_ID) 
		{
			return newDialog();
		}
		return null;
	}
}
