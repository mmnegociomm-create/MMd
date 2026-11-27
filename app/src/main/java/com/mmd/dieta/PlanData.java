package com.mmd.dieta;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class PlanData {
    private PlanData() {}
    public static final int DAYS = 15;
    public static final String[] NAMES = {"Café da manhã", "Almoço", "Lanche", "Jantar"};
    public static final String[] TIMES = {"07:00", "12:00", "16:00", "19:00"};
    public static final String[] WATER_TIMES = {"07:00","08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:30","18:00"};
    // Porções ajustadas para aproximadamente 2.000 kcal/dia. Valores são estimativas.
    public static final String[][] FOOD = {
        {"3 ovos, pão de leite (100 g), maçã (130 g)",
         "Arroz branco cozido (180 g), carne moída cozida (180 g), farinha de Cruzeiro do Sul (20 g)",
         "Aveia em flocos (50 g), composto lácteo em pó (30 g)",
         "Batata inglesa cozida (250 g), 2 ovos"},
        {"Cuscuz (80 g de Milharina seca), 3 ovos, manteiga (5 g)",
         "Arroz branco cozido (170 g), carne moída cozida (160 g), farinha (15 g)",
         "Maçã (150 g), aveia (40 g), composto lácteo (25 g)",
         "Batata inglesa cozida (200 g), 2 ovos, pão de leite (40 g)"},
        {"Pão de leite (100 g), 3 ovos",
         "Batata inglesa cozida (250 g), carne moída cozida (160 g), farinha (15 g)",
         "Maçã (150 g), aveia (50 g), composto lácteo (25 g)",
         "Arroz branco cozido (170 g), 2 ovos, manteiga (5 g)"}
    };
    public static final int[][] KCAL = {{584,756,327,362},{540,675,344,438},{516,672,383,401}};
    private static final String[][] NEW_FOOD={
        {"3 ovos, pão integral (60 g), iogurte natural proteico (170 g), maçã (130 g)",
         "Peito de frango grelhado (180 g), arroz (150 g), feijão (100 g), legumes (150 g), azeite (5 g)",
         "Iogurte natural proteico (170 g), aveia (40 g), banana (100 g)",
         "Tilápia grelhada (180 g), batata inglesa (220 g), legumes (150 g), azeite (5 g)"},
        {"3 ovos, cuscuz (50 g de flocão seco), iogurte natural proteico (170 g), banana (80 g)",
         "Patinho moído magro (150 g), arroz (130 g), feijão (100 g), legumes (150 g), azeite (5 g)",
         "Iogurte natural proteico (170 g), aveia (35 g), maçã (150 g)",
         "Peito de frango (160 g), batata inglesa (200 g), legumes (150 g), azeite (5 g)"},
        {"2 ovos, pão integral (60 g), cottage (100 g), banana (100 g)",
         "Tilápia grelhada (200 g), arroz (180 g), feijão (120 g), legumes (150 g), azeite (5 g)",
         "Iogurte natural proteico (170 g), aveia (50 g), maçã (150 g)",
         "Patinho moído magro (150 g), batata inglesa (200 g), legumes (150 g), azeite (5 g)"}
    };
    private static final int[][] NEW_KCAL={{534,663,345,495},{567,661,314,513},{481,675,373,519}};
    private static final int[] NEW_PROTEIN={167,163,156};
    public static boolean newPlan(Context c){return prefs(c).getBoolean("plan_v2",false);}
    public static void beginNew(Context c){prefs(c).edit().putBoolean("plan_v2",true).putString("start_date_v2",LocalDate.now().toString()).apply();}
    public static String food(Context c,int day,int meal){return (newPlan(c)?NEW_FOOD:FOOD)[type(day)][meal];}
    public static int kcal(Context c,int day,int meal){return (newPlan(c)?NEW_KCAL:KCAL)[type(day)][meal];}
    public static int protein(int day){return NEW_PROTEIN[type(day)];}
    public static int dayTotal(Context c,int day){int n=0;for(int m=0;m<4;m++)n+=kcal(c,day,m);return n;}
    public static int type(int day) { return (day - 1) % 3; }
    public static int dayTotal(int day) { int n=0; for(int k:KCAL[type(day)]) n+=k; return n; }
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("dieta15_state", Context.MODE_PRIVATE); }
    public static LocalDate start(Context c) {
        SharedPreferences p=prefs(c); String startKey=newPlan(c)?"start_date_v2":"start_date"; String v=p.getString(startKey, "");
        if(v.isEmpty()) { v=LocalDate.now().toString(); p.edit().putString(startKey,v).apply(); }
        return LocalDate.parse(v);
    }
    public static int today(Context c) {
        long d=ChronoUnit.DAYS.between(start(c),LocalDate.now())+1;
        return (int)Math.max(1,Math.min(15,d));
    }
    public static LocalDate date(Context c,int day) { return start(c).plusDays(day-1); }
    public static String key(Context c,int day,String suffix) { return (newPlan(c)?"v2_":"")+date(c,day)+"_"+suffix; }
    public static boolean mealDone(Context c,int day,int meal) {
        return prefs(c).getBoolean(key(c,day,"meal"+meal),false);
    }
    public static void setMeal(Context c,int day,int meal,boolean done) {
        prefs(c).edit().putBoolean(key(c,day,"meal"+meal),done).apply();
    }
    public static int waterSlotAmount(Context c,int slot) {
        int goal=goal(c);
        int base=(goal/11/50)*50;
        return slot==10?goal-base*10:base;
    }
    public static boolean waterDone(Context c,int day,int slot) {
        return prefs(c).getBoolean(key(c,day,"water_slot_"+slot),false);
    }
    public static void setWaterDone(Context c,int day,int slot,boolean done) {
        if(waterDone(c,day,slot)==done)return;
        prefs(c).edit().putBoolean(key(c,day,"water_slot_"+slot),done).apply();
        addWater(c,day,done?waterSlotAmount(c,slot):-waterSlotAmount(c,slot));
    }
    public static int water(Context c,int day) { return prefs(c).getInt(key(c,day,"water"),0); }
    public static void addWater(Context c,int day,int amount) {
        int n=Math.max(0,Math.min(15000,water(c,day)+amount));
        prefs(c).edit().putInt(key(c,day,"water"),n).apply();
    }
    public static float weight(Context c) { return prefs(c).getFloat("weight",111f); }
    public static void setWeight(Context c,float weight) { prefs(c).edit().putFloat("weight",weight).apply(); }
    public static int goal(Context c) { return Math.round(weight(c)*30f/50f)*50; }
    public static int doneCount(Context c,int day) {
        int n=0;for(int m=0;m<4;m++)if(mealDone(c,day,m))n++; return n;
    }
    public static String fmt(float n) { return String.format(new Locale("pt","BR"),"%.1f",n); }
}
