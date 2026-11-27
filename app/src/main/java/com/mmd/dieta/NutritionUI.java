package com.mmd.dieta;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.LocalDate;
import java.util.*;

final class NutritionUI {
    final MainActivity a; final SharedPreferences prefs;
    static final int INK=0xffedf7ff,MUTED=0xffa3b8d0,CYAN=0xff18d7ff,GREEN=0xff29eab8,ORANGE=0xffffb86b,CARD=0xff10243a;
    static final String[] ACT={"Pouco ativo • 1,20","Levemente ativo • 1,375","Moderadamente ativo • 1,55","Muito ativo • 1,725"};
    static final double[] FACTOR={1.2,1.375,1.55,1.725};
    static final int[] DEF={0,10,15,20};
    NutritionUI(MainActivity a){this.a=a;prefs=PlanData.prefs(a);}
    int dp(int v){return (int)(v*a.getResources().getDisplayMetrics().density+.5f);}
    String n(double v){return String.valueOf(Math.round(v));}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    void line(LinearLayout c,String s){TextView t=text(s,13,MUTED,false);t.setPadding(0,dp(4),0,dp(4));c.addView(t);}
    GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
    Button button(LinearLayout c,String s,Runnable f){Button b=new Button(a);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setBackground(bg(0xff193b57));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.topMargin=dp(7);c.addView(b,p);b.setOnClickListener(v->f.run());return b;}
    LinearLayout vertical(){LinearLayout l=new LinearLayout(a);l.setOrientation(1);return l;}
    LinearLayout sub(LinearLayout c){LinearLayout l=vertical();l.setPadding(dp(12),dp(10),dp(12),dp(10));l.setBackground(bg(CARD));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);c.addView(l,p);return l;}
    void bar(LinearLayout c,String label,double actual,double goal,int color,String suffix){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(label,13,INK,true),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(n(actual)+" / "+n(goal)+" "+suffix,13,color,true));c.addView(row);ProgressBar p=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress((int)Math.min(1000,Math.max(0,actual/Math.max(1,goal)*1000)));p.setProgressTintList(android.content.res.ColorStateList.valueOf(color));p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff2a405a));c.addView(p,new LinearLayout.LayoutParams(-1,dp(9)));}
    void dashboard(LinearLayout c,int day){
        NutritionData.Goal g=NutritionData.dayGoal(a,day);NutritionData.Totals t=NutritionData.day(a,day,true);
        line(c,"DIA "+day+" / "+PlanData.days(a)+" • "+PlanData.date(a,day)+" • Dados registrados");
        LinearLayout tile=sub(c);
        tile.addView(text(n(t.kcal)+" kcal",32,INK,true));line(tile,"Registradas • meta "+g.kcal+" kcal");
        bar(tile,"Energia",t.kcal,g.kcal,CYAN,"kcal");
        line(tile,t.kcal>g.kcal?n(t.kcal-g.kcal)+" kcal acima da referência":n(g.kcal-t.kcal)+" kcal restantes");
        if(t.known){bar(tile,"Proteína",t.p,g.p,GREEN,"g");bar(tile,"Carboidratos",t.c,g.c,CYAN,"g");bar(tile,"Gorduras",t.f,g.f,ORANGE,"g");}
        else {line(tile,"Macros incompletos: "+n(t.p)+" g proteína • "+n(t.c)+" g carboidratos • "+n(t.f)+" g gordura em refeições personalizadas. Cardápio antigo não tem macros por refeição.");}
        LinearLayout water=sub(c);bar(water,"Água",PlanData.water(a,day),NutritionData.waterGoal(a,day),CYAN,"ml");bar(water,"Refeições",PlanData.doneCount(a,day),4,GREEN,"");
        LocalDate date=PlanData.date(a,day);int run=NutritionData.workouts(a,date,"run"),gym=NutritionData.workouts(a,date,"gym");
        line(water,"Treinos concluídos registrados: "+run+" corrida • "+gym+" força");line(water,"Peso atual: "+PlanData.fmt(PlanData.weight(a))+" kg");float waist=prefs.getFloat("fit_m0",0);if(waist>0)line(water,"Última cintura: "+PlanData.fmt(waist)+" cm");
        button(c,"Calculadora • calorias e macros",()->calculator(day));
        button(c,"Metas e evolução • ciclo atual",()->report(day));
        button(c,"Montar dieta personalizada",()->a.openDiet());
        line(c,"Os números representam registros, não medições diretas de gordura ou gasto energético.");
    }
    void report(int selected){
        LinearLayout c=vertical();c.setPadding(dp(16),dp(8),dp(16),dp(12));ScrollView scroll=new ScrollView(a);scroll.addView(c);
        int water=0,energy=0,protein=0,proteinKnown=0,logged=0,runs=0,gyms=0,elapsed=0;LocalDate now=LocalDate.now();
        for(int d=1;d<=PlanData.days(a);d++){LocalDate date=PlanData.date(a,d);if(date.isAfter(now))continue;elapsed++;NutritionData.Totals t=NutritionData.day(a,d,true);NutritionData.Goal g=NutritionData.dayGoal(a,d);if(PlanData.water(a,d)>=NutritionData.waterGoal(a,d))water++;if(PlanData.doneCount(a,d)==4){logged++;if(Math.abs(t.kcal-g.kcal)<=g.kcal*.10)energy++;if(t.known){proteinKnown++;if(t.p>=g.p)protein++;}}runs+=NutritionData.workouts(a,date,"run");gyms+=NutritionData.workouts(a,date,"gym");}
        line(c,"Ciclo atual • "+elapsed+" dias transcorridos. Metas antigas sem referência salva usam a meta atual.");
        bar(c,"Hidratação atingida",water,Math.max(1,elapsed),CYAN,"dias");
        bar(c,"Calorias na faixa ±10%",energy,Math.max(1,elapsed),GREEN,"dias");
        bar(c,"Registro completo",logged,Math.max(1,elapsed),CYAN,"dias");
        if(proteinKnown>0)bar(c,"Proteína atingida",protein,proteinKnown,GREEN,"dias avaliáveis");else line(c,"Proteína: sem dias completos com macros conhecidos.");
        line(c,"Sessões desde esta atualização: "+runs+" corrida • "+gyms+" musculação");
        c.addView(text("EVOLUÇÃO DO PESO",18,INK,true));
        TreeMap<LocalDate,Double> values=new TreeMap<>();
        for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("fit_measure_"))try{LocalDate date=LocalDate.parse(en.getKey().substring(12));String val=String.valueOf(en.getValue());double w=Double.parseDouble(val.substring(0,val.indexOf(" kg")).replace(',','.'));if(Double.isFinite(w))values.put(date,w);}catch(Exception ignored){}
        if(values.isEmpty())line(c,"Registre peso na aba Corpo para iniciar o gráfico.");
        else {List<Map.Entry<LocalDate,Double>> list=new ArrayList<>(values.entrySet());if(list.size()>30)list=list.subList(list.size()-30,list.size());c.addView(new Trend(a,new ArrayList<>(list)),new LinearLayout.LayoutParams(-1,dp(180)));double first=values.firstEntry().getValue(),last=values.lastEntry().getValue();line(c,"Primeiro: "+String.format(Locale.US,"%.1f",first)+" kg • Último: "+String.format(Locale.US,"%.1f",last)+" kg");line(c,"Variação: "+String.format(Locale.US,"%+.1f",last-first)+" kg • "+values.size()+" registros");double sum=0;int count=0;for(Map.Entry<LocalDate,Double> en:values.entrySet())if(!en.getKey().isBefore(values.lastKey().minusDays(6))){sum+=en.getValue();count++;}line(c,"Média dos registros dos últimos 7 dias: "+String.format(Locale.US,"%.1f",sum/count)+" kg ("+count+" medições)");}
        c.addView(text("EVOLUÇÃO DAS MEDIDAS",18,INK,true));String first=prefs.getString("fit_measure_"+(values.isEmpty()?"":values.firstKey()),"");String last=prefs.getString("fit_measure_"+(values.isEmpty()?"":values.lastKey()),"");
        if(!first.isEmpty()){line(c,"Inicial: "+first);line(c,"Mais recente: "+last);}else line(c,"Nenhuma medida registrada.");
        new AlertDialog.Builder(a).setTitle("Relatório de metas").setView(scroll).setPositiveButton("Fechar",null).show();
    }
    static final class Trend extends View{
        final List<Map.Entry<LocalDate,Double>> data;final Paint p=new Paint(3);
        Trend(android.content.Context c,List<Map.Entry<LocalDate,Double>> d){super(c);data=d;setContentDescription("Gráfico dos registros reais de peso");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),left=43,right=w-18,top=24,bottom=h-32;if(right<=left||bottom<=top)return;
            double lo=Double.MAX_VALUE,hi=-Double.MAX_VALUE;for(Map.Entry<LocalDate,Double> e:data){lo=Math.min(lo,e.getValue());hi=Math.max(hi,e.getValue());}if(hi-lo<1){hi+=.5;lo-=.5;}
            p.setColor(0xff38536c);p.setStrokeWidth(1);p.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);for(int i=0;i<=3;i++){float y=top+(bottom-top)*i/3;p.setColor(0xff38536c);c.drawLine(left,y,right,y,p);p.setColor(MUTED);c.drawText(String.format(Locale.US,"%.1f",hi-(hi-lo)*i/3),2,y-3,p);}
            long start=data.get(0).getKey().toEpochDay(),end=data.get(data.size()-1).getKey().toEpochDay();float lastX=0,lastY=0;for(int i=0;i<data.size();i++){Map.Entry<LocalDate,Double> e=data.get(i);float x=end==start?(left+right)/2:left+(right-left)*(e.getKey().toEpochDay()-start)/(end-start);float y=(float)(bottom-(bottom-top)*(e.getValue()-lo)/(hi-lo));p.setColor(CYAN);p.setStrokeWidth(3);if(i>0)c.drawLine(lastX,lastY,x,y,p);c.drawCircle(x,y,5,p);lastX=x;lastY=y;}p.setColor(MUTED);c.drawText(data.get(0).getKey().toString().substring(5),left,h-6,p);if(data.size()>1)c.drawText(data.get(data.size()-1).getKey().toString().substring(5),right-40,h-6,p);
        }
    }
    EditText field(LinearLayout c,String label,String value,boolean decimal){line(c,label);EditText e=new EditText(a);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|(decimal?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setText(value);c.addView(e);return e;}
    double val(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
    Spinner spinner(LinearLayout c,String[] labels,int sel){Spinner s=new Spinner(a);ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels);s.setAdapter(ad);s.setSelection(Math.max(0,Math.min(labels.length-1,sel)));c.addView(s);return s;}
    void calculator(int day){
        LinearLayout c=vertical();c.setPadding(dp(16),0,dp(16),dp(12));ScrollView scroll=new ScrollView(a);scroll.addView(c);
        EditText w=field(c,"Peso atual (kg)",Float.toString(PlanData.weight(a)),true),h=field(c,"Altura (cm)",prefs.getFloat("fit_height",0)>0?Float.toString(prefs.getFloat("fit_height",0)):"",true),age=field(c,"Idade (anos)",prefs.getFloat("fit_age",0)>0?Float.toString(prefs.getFloat("fit_age",0)):"",true);
        line(c,"Sexo usado na fórmula");Spinner sex=spinner(c,new String[]{"Selecione","Masculino","Feminino"},prefs.getInt("fit_sex",-1)+1);
        line(c,"Atividade habitual, incluindo corrida e força (sem contar duas vezes)");Spinner activity=spinner(c,ACT,prefs.getInt("nut_activity",1));
        line(c,"Objetivo • déficit moderado");Spinner deficit=spinner(c,new String[]{"Manutenção","Déficit 10%","Déficit 15%","Déficit 20%"},prefs.getInt("nut_deficit",2));
        line(c,"Medidas ajudam no acompanhamento, mas não tornam o gasto energético exato. Fórmula Mifflin–St Jeor × atividade.");
        TextView result=text("Calcule para ver uma sugestão.",16,CYAN,true);c.addView(result);
        NutritionData.Goal old=NutritionData.goal(a);
        EditText kcal=field(c,"Meta energética (kcal)",String.valueOf(old.kcal),false),protein=field(c,"Proteína (g)",String.valueOf(old.p),false),carbs=field(c,"Carboidratos (g)",String.valueOf(old.c),false),fat=field(c,"Gorduras (g)",String.valueOf(old.f),false);
        
        Runnable calc=()->{try{double ww=val(w),hh=val(h),aa=val(age);int sx=sex.getSelectedItemPosition()-1;if(!Double.isFinite(ww+hh+aa)||ww<30||ww>350||hh<100||hh>250||aa<18||aa>100||sx<0)throw new Exception();double basal=NutritionData.bmr(ww,hh,aa,sx),tdee=basal*FACTOR[activity.getSelectedItemPosition()];NutritionData.Goal g=NutritionData.suggested(tdee,DEF[deficit.getSelectedItemPosition()]);result.setText("TMB: "+n(basal)+" kcal • Gasto total: "+n(tdee)+" kcal/dia\nSugestão: "+g.kcal+" kcal/dia");kcal.setText(""+g.kcal);protein.setText(""+g.p);carbs.setText(""+g.c);fat.setText(""+g.f);}catch(Exception e){Toast.makeText(a,"Confira peso, altura, idade e sexo.",Toast.LENGTH_SHORT).show();}};
        button(c,"Calcular sugestão 30% P / 40% C / 30% G",calc);
        line(c,"Metas editáveis. 4 kcal/g proteína e carboidrato; 9 kcal/g gordura. Ajuste com profissional se houver restrição médica ou doença renal.");
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Calculadora de energia").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar metas",null).create();d.show();
        d.getButton(-1).setOnClickListener(v->{try{double ww=val(w),hh=val(h),aa=val(age);int sx=sex.getSelectedItemPosition()-1;if(!Double.isFinite(ww+hh+aa)||ww<30||ww>350||hh<100||hh>250||aa<18||aa>100||sx<0)throw new Exception();int k=(int)val(kcal),p=(int)val(protein),cc=(int)val(carbs),f=(int)val(fat);double tdee=NutritionData.bmr(ww,hh,aa,sx)*FACTOR[activity.getSelectedItemPosition()];if(k<(sx==0?1500:1200)||k>6500||p<=0||cc<0||f<=0||Math.abs(k-(4.0*p+4.0*cc+9.0*f))>25||k<tdee*.75){Toast.makeText(a,"Confira os macros, calorias e evite déficit acima de 25%.",Toast.LENGTH_LONG).show();return;}PlanData.setWeight(a,(float)ww);prefs.edit().putFloat("fit_height",(float)hh).putFloat("fit_age",(float)aa).putInt("fit_sex",sx).apply();NutritionData.saveGoal(a,new NutritionData.Goal(k,p,cc,f),activity.getSelectedItemPosition(),deficit.getSelectedItemPosition());int today=PlanData.today(a);if(PlanData.date(a,today).equals(LocalDate.now())){prefs.edit().putString(PlanData.key(a,today,"nut_goal"),k+","+p+","+cc+","+f).putInt(PlanData.key(a,today,"nut_water_goal"),PlanData.goal(a)).apply();}ReminderScheduler.setup(a);ReminderScheduler.refreshWater(a,today);d.dismiss();a.render();Toast.makeText(a,"Metas salvas para hoje e dias futuros sem meta registrada.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(a,"Preencha os dados corretamente.",Toast.LENGTH_SHORT).show();}});
    }
    void planner(LinearLayout c,int day){
        NutritionData.Goal g=NutritionData.dayGoal(a,day);NutritionData.Totals t=NutritionData.day(a,day,false);
        line(c,"Meta do dia: "+g.kcal+" kcal • P "+g.p+" g • C "+g.c+" g • G "+g.f+" g");
        line(c,"Planejado: "+n(t.kcal)+" kcal"+(t.known?" • P "+n(t.p)+" • C "+n(t.c)+" • G "+n(t.f)+" g":" • macros ainda incompletos"));
        button(c,PlanData.natural(a)?"Gerar cardápio dos meus alimentos":"Ativar cardápio dos meus alimentos",()->{if(PlanData.natural(a))template(day);else a.activateNatural();});
        button(c,"Copiar cardápio personalizado para dias futuros",()->copy(day));
        line(c,"Edite cada refeição em gramas; dados nutricionais são aproximados. Confirme apenas o que foi realmente consumido.");
    }
    void template(int day){
        if(PlanData.natural(a)){new AlertDialog.Builder(a).setTitle("Gerar cardápio dos seus alimentos?").setMessage("Substitui as refeições não confirmadas deste dia por bisteca bovina, carne moída, macaxeira, batata, arroz, farinha, pão de leite e leite em pó integral. Refeições confirmadas são mantidas.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(d,w)->{for(int m=0;m<4;m++)if(!PlanData.mealDone(a,day,m))NutritionData.clear(a,day,m);a.render();}).show();return;}
        new AlertDialog.Builder(a).setTitle("Modelo ajustado às calorias").setMessage("Cria quatro refeições editáveis com frango, ovos, iogurte, arroz, feijão, frutas e gorduras. Porções são escaladas pela energia, não garantem coincidência exata com os macros. Refeições confirmadas são preservadas.").setNegativeButton("Cancelar",null).setPositiveButton("Gerar",(d,w)->{
            int[][] ids={{3,11,4,13},{0,6,7,15,16},{4,10,14,17},{2,8,15,16}};
            int[][] grams={{150,60,170,100},{180,150,100,150,5},{170,40,150,15},{180,220,150,5}};
            double base=0;for(int m=0;m<4;m++)for(int i=0;i<ids[m].length;i++)base+=NutritionData.FOODS[ids[m][i]].kcal*grams[m][i]/100;
            double scale=NutritionData.dayGoal(a,day).kcal/base;
            for(int m=0;m<4;m++)if(!PlanData.mealDone(a,day,m)){JSONArray list=new JSONArray();for(int i=0;i<ids[m].length;i++)list.put(NutritionData.entry(NutritionData.FOODS[ids[m][i]],Math.max(1,Math.round(grams[m][i]*scale))));NutritionData.save(a,day,m,list);}
            a.render();
        }).show();
    }
    void copy(int day){
        boolean any=false;for(int m=0;m<4;m++)any|=NutritionData.has(a,day,m);if(!any){Toast.makeText(a,"Monte uma refeição primeiro.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(a).setTitle("Copiar para dias futuros?").setMessage("Replica as refeições personalizadas somente nos dias futuros do ciclo, sem alterar refeições confirmadas ou já personalizadas.").setNegativeButton("Cancelar",null).setPositiveButton("Copiar",(d,w)->{for(int i=day+1;i<=PlanData.days(a);i++)if(!PlanData.date(a,i).isBefore(LocalDate.now()))for(int m=0;m<4;m++)if(NutritionData.has(a,day,m)&&!NutritionData.has(a,i,m)&&!PlanData.mealDone(a,i,m))NutritionData.save(a,i,m,NutritionData.read(a,day,m));a.render();Toast.makeText(a,"Cardápio copiado.",Toast.LENGTH_SHORT).show();}).show();
    }
    void editMeal(int day,int m){
        if(PlanData.mealDone(a,day,m)){Toast.makeText(a,"Desmarque a refeição para editar.",Toast.LENGTH_SHORT).show();return;}
        JSONArray list=NutritionData.effectiveMeal(a,day,m);
        LinearLayout c=vertical();c.setPadding(dp(16),dp(4),dp(16),dp(12));ScrollView scroll=new ScrollView(a);scroll.addView(c);
        LinearLayout entries=vertical();c.addView(entries);
        
        // Repaint recursively without replacing the temporary list or saving until the user confirms.
        repaintProxy(entries,list,day,m);
        button(c,"Adicionar alimento da biblioteca",()->{String[] names=new String[NutritionData.FOODS.length];for(int i=0;i<names.length;i++)names[i]=NutritionData.FOODS[i].name;new AlertDialog.Builder(a).setTitle("Escolher alimento").setItems(names,(d,i)->{LinearLayout box=vertical();box.setPadding(dp(16),0,dp(16),0);EditText grams=field(box,"Quantidade (g)","100",true);AlertDialog g=new AlertDialog.Builder(a).setTitle(names[i]).setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Adicionar",null).create();g.show();g.getButton(-1).setOnClickListener(v->{try{double amount=val(grams);if(!Double.isFinite(amount)||amount<=0||amount>3000)throw new Exception();list.put(NutritionData.entry(NutritionData.FOODS[i],amount));repaintProxy(entries,list,day,m);g.dismiss();}catch(Exception e){grams.setError("Informe gramas válidas");}});}).show();});
        button(c,"Adicionar alimento pelo rótulo",()->customFood(list,entries,day,m));
        
        AlertDialog d=new AlertDialog.Builder(a).setTitle(PlanData.NAMES[m]).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar refeição",null).setNeutralButton("Restaurar original",null).create();d.show();d.getButton(-3).setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Restaurar cardápio original?").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(x,y)->{NutritionData.clear(a,day,m);d.dismiss();a.render();}).show());d.getButton(-1).setOnClickListener(v->{if(list.length()==0){Toast.makeText(a,"Adicione pelo menos um alimento.",Toast.LENGTH_SHORT).show();return;}NutritionData.save(a,day,m,list);d.dismiss();a.render();});
    }
    void repaintProxy(LinearLayout c,JSONArray list,int day,int m){c.removeAllViews();NutritionData.Totals t=NutritionData.totals(list);c.addView(text(n(t.kcal)+" kcal • P "+n(t.p)+" • C "+n(t.c)+" • G "+n(t.f)+" g",16,CYAN,true));for(int i=0;i<list.length();i++){final int j=i;JSONObject obj=list.optJSONObject(i);if(obj!=null)button(c,obj.optString("name")+" • "+n(obj.optDouble("g"))+" g  ✎",()->editPortion(c,list,day,m,j,obj));}if(list.length()==0)line(c,"Adicione os alimentos da sua refeição.");}
    void editPortion(LinearLayout c,JSONArray list,int day,int m,int j,JSONObject obj){
        LinearLayout box=vertical();box.setPadding(dp(16),0,dp(16),0);EditText grams=field(box,"Quantidade (g)",String.valueOf(obj.optDouble("g")),true);AlertDialog d=new AlertDialog.Builder(a).setTitle(obj.optString("name")).setView(box).setNegativeButton("Cancelar",null).setNeutralButton("Remover",(x,y)->{list.remove(j);repaintProxy(c,list,day,m);}).setPositiveButton("Salvar",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{double g=val(grams);if(!Double.isFinite(g)||g<=0||g>3000)throw new Exception();obj.put("g",g);repaintProxy(c,list,day,m);d.dismiss();}catch(Exception e){grams.setError("Informe gramas válidas");}});
    }
    void customFood(JSONArray list,LinearLayout entries,int day,int m){
        LinearLayout c=vertical();c.setPadding(dp(16),0,dp(16),0);ScrollView s=new ScrollView(a);s.addView(c);EditText name=new EditText(a);name.setHint("Nome do alimento");c.addView(name);EditText grams=field(c,"Porção consumida (g)","100",true),k=field(c,"kcal por 100 g","",true),p=field(c,"Proteína por 100 g","",true),cc=field(c,"Carboidratos por 100 g","",true),f=field(c,"Gorduras por 100 g","",true);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Dados do rótulo").setView(s).setNegativeButton("Cancelar",null).setPositiveButton("Adicionar",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{String nm=name.getText().toString().trim();double g=val(grams),kk=val(k),pp=val(p),ccc=val(cc),ff=val(f);if(nm.isEmpty()||!Double.isFinite(g+kk+pp+ccc+ff)||g<=0||g>3000||kk<0||kk>950||pp<0||ccc<0||ff<0||pp+ccc+ff>105)throw new Exception();list.put(NutritionData.entry(nm,g,kk,pp,ccc,ff));repaintProxy(entries,list,day,m);d.dismiss();}catch(Exception e){Toast.makeText(a,"Confira os valores por 100 g.",Toast.LENGTH_SHORT).show();}});
    }
}