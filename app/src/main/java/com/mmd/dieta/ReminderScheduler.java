package com.mmd.dieta;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ReminderScheduler {
    private ReminderScheduler() {}
    public static final String ACTION_ALERT="com.mmd.dieta.ALERT";
    public static final String ACTION_CONFIRM="com.mmd.dieta.CONFIRM";
    public static final String EXTRA_DAY="day";
    public static final String EXTRA_SLOT="slot";
    public static final int WATER_START=10;
    private static PendingIntent intent(Context c,int day,int slot,boolean repeat) {
        Intent i=new Intent(c,ReminderReceiver.class).setAction(ACTION_ALERT);
        i.putExtra(EXTRA_DAY,day).putExtra(EXTRA_SLOT,slot);
        int code=1000+day*100+slot+(repeat?3000:0);
        return PendingIntent.getBroadcast(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static AlarmManager alarms(Context c) { return (AlarmManager)c.getSystemService(Context.ALARM_SERVICE); }
    private static void at(Context c,int day,int slot,boolean repeat,long millis) {
        if(millis<=System.currentTimeMillis())return;
        AlarmManager a=alarms(c);
        if(a!=null)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,intent(c,day,slot,repeat));
    }
    public static void setup(Context c) {
        PlanData.start(c);
        for(int day=1;day<=PlanData.DAYS;day++) {
            for(int slot=0;slot<4+PlanData.WATER_TIMES.length;slot++) {
                boolean water=slot>=WATER_START;
                if(slot>=4 && slot<WATER_START)continue;
                if(!water && PlanData.mealDone(c,day,slot))continue;
                if(water && PlanData.water(c,day)>=PlanData.goal(c))continue;
                String time=water?PlanData.WATER_TIMES[slot-WATER_START]:PlanData.TIMES[slot];
                LocalDateTime dateTime=LocalDateTime.of(PlanData.date(c,day),LocalTime.parse(time));
                at(c,day,slot,false,dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
        }
    }
    public static void repeatMeal(Context c,int day,int meal) {
        if(PlanData.mealDone(c,day,meal) || !PlanData.date(c,day).equals(java.time.LocalDate.now()))return;
        LocalTime cutoff=LocalTime.of(22,0);
        if(LocalTime.now().isAfter(cutoff.minusMinutes(25)))return;
        at(c,day,meal,true,System.currentTimeMillis()+25*60*1000L);
    }
    public static void cancelMeal(Context c,int day,int meal) {
        AlarmManager a=alarms(c);
        if(a!=null) {
            a.cancel(intent(c,day,meal,false));
            a.cancel(intent(c,day,meal,true));
        }
        android.app.NotificationManager n=(android.app.NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(n!=null)n.cancel(20000+day*100+meal);
    }
}
