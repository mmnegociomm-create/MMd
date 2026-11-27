package com.mmd.dieta;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

final class NutritionData {
    static final class Food {
        final String name; final double kcal,p,c,f;
        Food(String n,double k,double p,double c,double f){name=n;kcal=k;this.p=p;this.c=c;this.f=f;}
    }
    // Approximate per 100 g, plain preparation without added oil unless specified.
    static final Food[] FOODS={
        new Food("Peito de frango grelhado",165,31,0,3.6),
        new Food("Patinho magro cozido",210,30,0,10),
        new Food("Tilápia grelhada",128,26,0,2.7),
        new Food("Ovo inteiro cozido",155,13,1.1,10.6),
        new Food("Iogurte natural proteico (referência)",65,10,4,0.8),
        new Food("Cottage",98,11,3.4,4.3),
        new Food("Arroz branco cozido",130,2.7,28,0.3),
        new Food("Feijão carioca cozido",76,4.8,13.6,0.5),
        new Food("Batata inglesa cozida",87,1.9,20,0.1),
        new Food("Batata-doce cozida",86,1.6,20,0.1),
        new Food("Aveia seca",389,17,66,7),
        new Food("Pão integral",247,12,41,4),
        new Food("Flocão de milho seco",360,7,79,2),
        new Food("Banana",89,1.1,23,0.3),
        new Food("Maçã",52,0.3,14,0.2),
        new Food("Legumes variados cozidos",35,2,7,0.2),
        new Food("Azeite",884,0,0,100),
        new Food("Castanhas (referência)",600,17,20,53),
        new Food("Leite desnatado",35,3.4,5,0.1),
        new Food("Mandioca cozida",125,1,30,0.3),
        new Food("Atum em água drenado",116,26,0,1),
        new Food("Farinha de Cruzeiro do Sul (seca)",365,1.6,88,0.3),
        new Food("Leite em pó integral (referência)",496,26,38,27),
        new Food("Carne moída de segunda cozida (estimativa)",250,26,0,16)
    };
    static final class Totals {
        double kcal,p,c,f; boolean known=true;
        void add(Totals b){kcal+=b.kcal;p+=b.p;c+=b.c;f+=b.f;known&=b.known;}
    }
    static SharedPreferences prefs(Context c){return PlanData.prefs(c);}
    static String mealKey(Context c,int d,int m){return PlanData.key(c,d,"custom_meal_"+m);}
    static boolean has(Context c,int d,int m){return prefs(c).contains(mealKey(c,d,m));}
    static JSONArray read(Context c,int d,int m){try{return new JSONArray(prefs(c).getString(mealKey(c,d,m),"[]"));}catch(Exception e){return new JSONArray();}}
    static void save(Context c,int d,int m,JSONArray list){prefs(c).edit().putString(mealKey(c,d,m),list.toString()).apply();}
    static void clear(Context c,int d,int m){prefs(c).edit().remove(mealKey(c,d,m)).apply();}
    static JSONObject entry(String name,double grams,double k,double p,double carbs,double f){try{return new JSONObject().put("name",name).put("g",grams).put("k",k).put("p",p).put("c",carbs).put("f",f);}catch(Exception e){throw new IllegalArgumentException(e);}}
    static JSONObject entry(Food f,double g){return entry(f.name,g,f.kcal,f.p,f.c,f.f);}
    static Totals totals(JSONArray list){Totals t=new Totals();for(int i=0;i<list.length();i++){JSONObject j=list.optJSONObject(i);if(j==null)continue;double g=j.optDouble("g",0)/100;t.kcal+=g*j.optDouble("k",0);t.p+=g*j.optDouble("p",0);t.c+=g*j.optDouble("c",0);t.f+=g*j.optDouble("f",0);}return t;}
    static Totals meal(Context c,int d,int m){if(has(c,d,m))return totals(read(c,d,m));if(PlanData.natural(c))return totals(naturalMeal(c,d,m));Totals t=new Totals();t.kcal=PlanData.kcal(c,d,m);t.known=false;return t;}
    static Totals day(Context c,int d,boolean onlyDone){Totals t=new Totals();for(int m=0;m<4;m++)if(!onlyDone||PlanData.mealDone(c,d,m))t.add(meal(c,d,m));return t;}
    static JSONArray naturalMeal(Context c,int day,int meal){
        int[][][] ids={{{3,22,12,13},{0,6,7,15},{0,8,14,22},{23,19,21,15,17}},{{3,22,19,13},{23,6,7,15},{0,8,14,22},{0,12,15,17}},{{3,22,12,14},{0,6,7,15,21},{3,0,19,13},{23,8,15,17}}};
        int[][][] grams={{{150,30,60,100},{200,160,100,150},{120,230,150,20},{180,130,15,150,15}},{{150,30,160,100},{170,170,100,150},{180,200,150,20},{150,55,150,25}},{{150,30,55,150},{210,170,100,150,15},{100,100,130,100},{180,230,150,15}}};
        int t=(day-1)%3;double base=0;for(int m=0;m<4;m++)for(int j=0;j<ids[t][m].length;j++)base+=FOODS[ids[t][m][j]].kcal*grams[t][m][j]/100.0;
        double scale=dayGoal(c,day).kcal/base;JSONArray list=new JSONArray();
        for(int j=0;j<ids[t][meal].length;j++)list.put(entry(FOODS[ids[t][meal][j]],Math.max(1,Math.round(grams[t][meal][j]*scale))));
        return list;
    }
    static JSONArray effectiveMeal(Context c,int day,int meal){return has(c,day,meal)?read(c,day,meal):PlanData.natural(c)?naturalMeal(c,day,meal):new JSONArray();}
    static String description(JSONArray list){StringBuilder s=new StringBuilder();for(int i=0;i<list.length();i++){JSONObject j=list.optJSONObject(i);if(j!=null){if(s.length()>0)s.append("\n");s.append(j.optString("name")).append(" • ").append(Math.round(j.optDouble("g",0))).append(" g");}}return s.toString();}
    static final class Goal {
        final int kcal,p,c,f; Goal(int k,int p,int c,int f){kcal=k;this.p=p;this.c=c;this.f=f;}
    }
    static Goal goal(Context c){SharedPreferences p=prefs(c);return new Goal(p.getInt("nut_kcal",2000),p.getInt("nut_p",150),p.getInt("nut_c",215),p.getInt("nut_f",60));}
    static Goal dayGoal(Context c,int day){SharedPreferences p=prefs(c);String key=PlanData.key(c,day,"nut_goal");if(!p.contains(key))return goal(c);try{String[] v=p.getString(key,"").split(",");return new Goal(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]));}catch(Exception e){return goal(c);}}
    static void freeze(Context c,int day){String key=PlanData.key(c,day,"nut_goal");if(!prefs(c).contains(key)){Goal g=goal(c);prefs(c).edit().putString(key,g.kcal+","+g.p+","+g.c+","+g.f).putInt(PlanData.key(c,day,"nut_water_goal"),PlanData.goal(c)).apply();}}
    static int waterGoal(Context c,int day){return prefs(c).getInt(PlanData.key(c,day,"nut_water_goal"),PlanData.goal(c));}
    static double bmr(double w,double h,double age,int sex){return 10*w+6.25*h-5*age+(sex==0?5:-161);}
    static void workout(Context c,String type){String k="nut_workout_"+java.time.LocalDate.now()+"_"+type;prefs(c).edit().putInt(k,prefs(c).getInt(k,0)+1).apply();}
    static int workouts(Context c,java.time.LocalDate d,String type){return prefs(c).getInt("nut_workout_"+d+"_"+type,0);}
    static Goal suggested(double tdee,int percent){int k=(int)Math.round(tdee*(1-percent/100.0)/10)*10;int protein=(int)Math.round(k*.30/4),fat=(int)Math.round(k*.30/9);return new Goal(k,protein,(int)Math.round((k-4*protein-9*fat)/4.0),fat);}
    static void saveGoal(Context c,Goal g,int activity,int deficit){prefs(c).edit().putInt("nut_kcal",g.kcal).putInt("nut_p",g.p).putInt("nut_c",g.c).putInt("nut_f",g.f).putInt("nut_activity",activity).putInt("nut_deficit",deficit).apply();}
}