package com.mmd.dieta;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.LocalDate;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL="dieta_lembretes_v1";
    @Override public void onReceive(Context c,Intent i) {
        int day=i.getIntExtra(ReminderScheduler.EXTRA_DAY,-1);
        int slot=i.getIntExtra(ReminderScheduler.EXTRA_SLOT,-1);
        if(day<1||day>15||slot<0||slot>=21)return;
        if(ReminderScheduler.ACTION_CONFIRM.equals(i.getAction())) {
            if(slot<4) { PlanData.setMeal(c,day,slot,true);ReminderScheduler.cancelMeal(c,day,slot); }
            return;
        }
        if(!ReminderScheduler.ACTION_ALERT.equals(i.getAction()))return;
        if(!LocalDate.now().equals(PlanData.date(c,day)))return;
        boolean water=slot>=ReminderScheduler.WATER_START;
        if(!water&&PlanData.mealDone(c,day,slot))return;
        if(water&&PlanData.water(c,day)>=PlanData.goal(c))return;
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Refeições e hidratação",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Lembretes do plano Dieta 15 Dias");
        nm.createNotificationChannel(channel);
        Intent open=new Intent(c,MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String title=water?"Hora de beber água 💧":"Hora da refeição — "+PlanData.NAMES[slot];
        String body=water?"Registre a água consumida. Meta diária: "+PlanData.goal(c)+" ml.":"Dia "+day+" • "+PlanData.FOOD[PlanData.type(day)][slot];
        Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
            .setColor(0xff16d9ff).setAutoCancel(true).setContentIntent(content).setPriority(Notification.PRIORITY_HIGH);
        if(!water) {
            Intent confirm=new Intent(c,ReminderReceiver.class).setAction(ReminderScheduler.ACTION_CONFIRM);
            confirm.putExtra(ReminderScheduler.EXTRA_DAY,day).putExtra(ReminderScheduler.EXTRA_SLOT,slot);
            PendingIntent action=PendingIntent.getBroadcast(c,8000+day*100+slot,confirm,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(android.R.drawable.checkbox_on_background,"Concluí",action).build());
        }
        nm.notify(20000+day*100+slot,b.build());
        if(!water)ReminderScheduler.repeatMeal(c,day,slot);
    }
}
