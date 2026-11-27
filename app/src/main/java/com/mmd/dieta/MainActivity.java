package com.mmd.dieta;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=0xff050a16, CARD=0xff0d162b, BLUE=0xff18d7ff, WHITE=0xffecf7ff, MUTED=0xff96a8c5, LINE=0xff213453, SUCCESS=0xff29eab8;
    private LinearLayout root;
    private int chosen=1;
    private int tab=0;
    private FitnessUI fitness;
    private NutritionUI nutrition;
    private final DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy",new Locale("pt","BR"));
    private int dp(float n) {return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable drawable(int color,int stroke,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView v=new TextView(this);
        v.setText(s);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setIncludeFontPadding(true);
        return v;
    }
    private LinearLayout vertical() {LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h,int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));
        p.topMargin=dp(top);return p;
    }
    private void gap(LinearLayout l,int n) {View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private LinearLayout card(String title) {
        LinearLayout c=vertical();c.setPadding(dp(16),dp(17),dp(16),dp(17));
        c.setBackground(drawable(CARD,LINE,22));
        root.addView(c,lp(-1,-2,14));
        c.addView(text(title,18,WHITE,true));
        gap(c,12);
        return c;
    }
    private Button button(String label,boolean active) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);
        b.setTextColor(active?BG:WHITE);b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setPadding(dp(7),0,dp(7),0);
        b.setMinHeight(0);b.setMinimumHeight(0);
        b.setBackground(drawable(active?BLUE:0xff12213b,active?0:LINE,13));
        return b;
    }
    private void progress(LinearLayout into,int value,int max) {
        ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        b.setMax(Math.max(1,max));b.setProgress(Math.min(max,Math.max(0,value)));
        b.setProgressTintList(ColorStateList.valueOf(BLUE));
        b.setProgressBackgroundTintList(ColorStateList.valueOf(0xff20314d));
        into.addView(b,lp(-1,9,11));
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        PlanData.start(this);chosen=PlanData.today(this);fitness=new FitnessUI(this);nutrition=new NutritionUI(this);
        ReminderScheduler.setup(this);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
        render();
    }
    @Override protected void onResume(){super.onResume();if(root!=null)render();}
    @Override protected void onDestroy(){if(fitness!=null)fitness.destroy();super.onDestroy();}
    @Override public void onBackPressed(){if(fitness!=null&&fitness.busy()){new android.app.AlertDialog.Builder(this).setTitle("Treino em andamento").setMessage("Encerre pela tela de treino ou continue.").setPositiveButton("Continuar",null).show();}else if(tab!=0){tab=0;render();}else super.onBackPressed();}
    void render() {
        LinearLayout frame=vertical();frame.setBackgroundColor(BG);
        frame.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}return insets;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=vertical();root.setPadding(dp(17),dp(14),dp(17),dp(18));scroll.addView(root);
        frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(6),dp(5),dp(6),dp(5));nav.setBackgroundColor(CARD);
        String[] tabs={"Início","Dieta","Água","Corrida","Força","Corpo"};
        for(int i=0;i<tabs.length;i++){final int n=i;Button b=button(tabs[i],tab==i);b.setTextSize(11);b.setPadding(0,0,0,0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.leftMargin=dp(2);nav.addView(b,p);b.setOnClickListener(v->{if(fitness.busy()&&n!=tab){Toast.makeText(this,"Finalize o treino antes de mudar de tela.",Toast.LENGTH_SHORT).show();return;}tab=n;render();});}
        frame.addView(nav,new LinearLayout.LayoutParams(-1,-2));setContentView(frame);
        switch(tab){
            case 0: dayPicker();nutrition.dashboard(card("DASHBOARD • MINHAS METAS"),chosen);break;
            case 1: dayPicker();nutrition.planner(card("PLANEJAMENTO ALIMENTAR"),chosen);menu();break;
            case 2: dayPicker();hydration();break;
            case 3: fitness.runningScreen(card("CORRIDA GUIADA"));break;
            case 4: fitness.gymScreen(card("MUSCULAÇÃO GUIADA"));break;
            case 5: fitness.profile(card("EVOLUÇÃO CORPORAL"));break;
        }
    }
    void openDiet(){tab=1;render();}
    private void dayPicker(){LinearLayout c=card("Dia do protocolo");Button b=button("Dia "+chosen+" / 15  •  "+PlanData.date(this,chosen).format(fmt),true);c.addView(b,lp(-1,46,0));b.setOnClickListener(v->{String[] days=new String[15];for(int i=0;i<15;i++)days[i]="Dia "+(i+1)+" • "+PlanData.date(this,i+1).format(fmt);new android.app.AlertDialog.Builder(this).setTitle("Escolher dia").setSingleChoiceItems(days,chosen-1,(d,i)->{chosen=i+1;d.dismiss();render();}).setNegativeButton("Cancelar",null).show();});
    }
    private void header() {
        LinearLayout hero=vertical();hero.setPadding(dp(19),dp(20),dp(19),dp(19));
        GradientDrawable gd=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff103f63,0xff101e42,0xff0a1328});
        gd.setCornerRadius(dp(24));gd.setStroke(dp(1),0xff1d86ba);hero.setBackground(gd);
        root.addView(hero,lp(-1,-2,0));
        hero.addView(text("◆  PROTOCOLO 15D",12,BLUE,true));
        gap(hero,8);
        hero.addView(text("DIETA 15 DIAS",29,WHITE,true));
        hero.addView(text("NUTRIÇÃO • HIDRATAÇÃO • PROGRESSO",11,MUTED,true));
        gap(hero,15);
        hero.addView(text("DIA "+chosen+" / 15    •    "+PlanData.date(this,chosen).format(fmt),15,WHITE,true));
        progress(hero,PlanData.doneCount(this,chosen),4);
        hero.addView(text(PlanData.doneCount(this,chosen)+" de 4 refeições concluídas",12,MUTED,false));
    }
    private void calendar() {
        LinearLayout c=card("SELECIONE O DIA");
        for(int row=0;row<3;row++){
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
            c.addView(line,lp(-1,50,row==0?0:7));
            for(int col=0;col<5;col++){
                final int day=row*5+col+1;
                Button b=button("D"+day+"\n"+(char)('A'+PlanData.type(day)),day==chosen);
                b.setTextSize(12);b.setSingleLine(false);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
                if(col>0)p.leftMargin=dp(5);
                line.addView(b,p);
                b.setOnClickListener(v->{chosen=day;render();});
            }
        }
    }
    private void menu() {
        int type=PlanData.type(chosen),total=(int)Math.round(NutritionData.day(this,chosen,false).kcal);
        LinearLayout c=card("REFEIÇÕES DO DIA • ~"+total+" kcal");
        c.addView(text("Pesos cozidos, exceto aveia. Valores aproximados; ajuste com nutricionista.",12,MUTED,false));if(PlanData.newPlan(this)){c.addView(text("Cardápio base disponível; personalize por refeição.",12,MUTED,false),lp(-1,-2,8));}else{Button activate=button("Iniciar novo plano de 15 dias",true);c.addView(activate,lp(-1,48,8));activate.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Novo ciclo alimentar").setMessage("Começa hoje com cardápio de maior proteína. Registros antigos ficam armazenados, mas não serão exibidos neste ciclo.").setNegativeButton("Cancelar",null).setPositiveButton("Iniciar",(d,w)->{for(int j=1;j<=15;j++){for(int k=0;k<4;k++)ReminderScheduler.cancelMeal(this,j,k);for(int k=0;k<PlanData.WATER_TIMES.length;k++)ReminderScheduler.cancelWater(this,j,k);}PlanData.beginNew(this);chosen=1;ReminderScheduler.setup(this);render();}).show());}
        for(int m=0;m<4;m++) {
            final int index=m;
            LinearLayout sec=vertical();
            sec.setPadding(dp(12),dp(12),dp(12),dp(12));
            sec.setBackground(drawable(0xff0a2038,LINE,15));
            c.addView(sec,lp(-1,-2,m==0?13:9));
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
            sec.addView(line);
            TextView title=text(PlanData.TIMES[m]+"  "+PlanData.NAMES[m],14,WHITE,true);
            line.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            NutritionData.Totals meal=NutritionData.meal(this,chosen,m);
            TextView kcal=text("~"+Math.round(meal.kcal)+" kcal",12,BLUE,true);
            line.addView(kcal);
            TextView content=text(NutritionData.has(this,chosen,m)?NutritionData.description(NutritionData.read(this,chosen,m)):PlanData.food(this,chosen,m),13,MUTED,false);
            sec.addView(content,lp(-1,-2,8));
            if(meal.known)sec.addView(text("P "+Math.round(meal.p)+" g • C "+Math.round(meal.c)+" g • G "+Math.round(meal.f)+" g",12,SUCCESS,true),lp(-1,-2,6));
            boolean done=PlanData.mealDone(this,chosen,m);
            Button edit=button("Editar alimentos e gramas",false);sec.addView(edit,lp(-1,40,8));edit.setEnabled(!done);edit.setOnClickListener(v->nutrition.editMeal(chosen,index));
            Button confirm=button(done?"✓  Refeição concluída":"Confirmar refeição",done);
            sec.addView(confirm,lp(-1,42,10));
            confirm.setOnClickListener(v->{
                boolean newDone=!PlanData.mealDone(this,chosen,index);
                if(newDone)NutritionData.freeze(this,chosen);
                PlanData.setMeal(this,chosen,index,newDone);
                if(newDone)ReminderScheduler.cancelMeal(this,chosen,index);
                else ReminderScheduler.setup(this);
                render();
            });
        }
    }
    void registerWater(int day,int ml){
        int before=PlanData.water(this,day);PlanData.addWater(this,day,ml);
        PlanData.prefs(this).edit().putInt(PlanData.key(this,day,"nut_water_goal"),PlanData.goal(this)).apply();
        ReminderScheduler.refreshWater(this,day);
        int after=PlanData.water(this,day);
        Toast.makeText(this,(after>=before?"+":"")+(after-before)+" ml • Total: "+after+" ml",Toast.LENGTH_SHORT).show();
        render();
    }
    void customWater(int day){
        EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);input.setHint("Ex.: 1250");
        LinearLayout holder=vertical();holder.setPadding(dp(22),0,dp(22),0);holder.addView(input);
        android.app.AlertDialog d=new android.app.AlertDialog.Builder(this).setTitle("Registrar água (ml)")
            .setMessage("Informe quanto realmente bebeu, mesmo durante o treino. Não force água em excesso.").setView(holder)
            .setNegativeButton("Cancelar",null).setPositiveButton("Adicionar",null).setNeutralButton("Descontar",null).create();
        d.show();
        for(int which:new int[]{-1,-3})d.getButton(which).setOnClickListener(v->{try{
            int amount=Integer.parseInt(input.getText().toString().trim());if(amount<=0||amount>15000)throw new Exception();
            d.dismiss();registerWater(day,which==-1?amount:-amount);
        }catch(Exception e){input.setError("Informe um número válido em ml");}});
    }
    private void hydration() {
        int amount=PlanData.water(this,chosen),goal=PlanData.goal(this);
        LinearLayout c=card("HIDRATAÇÃO  💧  •  ATÉ 18H");
        c.addView(text(amount+" / "+goal+" ml",30,WHITE,true));
        c.addView(text(amount>=goal?"✓ Meta atingida • "+(amount-goal)+" ml acima da referência":"Faltam "+(goal-amount)+" ml  •  "+Math.round(amount*100f/goal)+"% da meta",13,amount>=goal?SUCCESS:MUTED,true));
        progress(c,amount,goal);
        c.addView(text("REGISTRAR AGORA",12,BLUE,true),lp(-1,-2,14));
        LinearLayout buttons=new LinearLayout(this);c.addView(buttons,lp(-1,48,8));
        int[] values={200,300,-200};String[] labels={"+200 ml","+300 ml","−200 ml"};
        for(int i=0;i<3;i++){
            final int ml=values[i];Button b=button(labels[i],i==1);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(i>0)p.leftMargin=dp(5);
            buttons.addView(b,p);b.setOnClickListener(v->registerWater(chosen,ml));
        }
        Button custom=button("＋ Informar quantidade em ml",true);c.addView(custom,lp(-1,48,8));custom.setOnClickListener(v->customWater(chosen));
        c.addView(text("Os registros livres já abatem o saldo. Os horários cobertos não geram novos lembretes.",12,MUTED,false),lp(-1,-2,8));
        int pending=0,doneCount=0,covered=0;for(int i=0;i<PlanData.WATER_TIMES.length;i++){if(PlanData.waterDone(this,chosen,i))doneCount++;else if(PlanData.plannedWater(this,chosen,i)==0)covered++;else pending++;}
        LinearLayout schedule=card("HORÁRIOS DE ÁGUA");
        schedule.addView(text(doneCount+" confirmados • "+covered+" abatidos • "+pending+" pendentes",13,MUTED,false));
        Button show=button("Ver / editar horários",false);schedule.addView(show,lp(-1,46,8));show.setOnClickListener(v->waterSchedule());
        c.addView(text("A meta é apenas referência. Beba conforme sede e condições do treino; evite grandes volumes de uma vez.",12,MUTED,false),lp(-1,-2,12));
    }
    private void waterSchedule(){
        ScrollView scroll=new ScrollView(this);LinearLayout body=vertical();body.setPadding(dp(16),dp(8),dp(16),dp(12));scroll.addView(body);
        android.app.AlertDialog d=new android.app.AlertDialog.Builder(this).setTitle("Horários • dia "+chosen).setView(scroll).setPositiveButton("Fechar",null).create();
        for(int i=0;i<PlanData.WATER_TIMES.length;i++){
            final int slot=i;boolean done=PlanData.waterDone(this,chosen,i);int ml=done?PlanData.confirmedWater(this,chosen,i):PlanData.plannedWater(this,chosen,i);
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);body.addView(line,lp(-1,48,4));
            TextView label=text(PlanData.WATER_TIMES[i]+" • "+(ml==0?"Abatido":ml+" ml"),14,done||ml==0?SUCCESS:WHITE,true);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            Button b=button(done?"Desfazer":ml==0?"Coberto":"Confirmar",done);line.addView(b,new LinearLayout.LayoutParams(dp(112),dp(42)));b.setEnabled(done||ml>0);
            b.setOnClickListener(v->{PlanData.setWaterDone(this,chosen,slot,!done);ReminderScheduler.refreshWater(this,chosen);d.dismiss();render();waterSchedule();});
        }
        d.show();
    }
    private void weightCard() {
        LinearLayout c=card("RECALCULAR PELO PESO");
        c.addView(text("Peso registrado: "+PlanData.fmt(PlanData.weight(this))+" kg",17,WHITE,true));
        c.addView(text("Ao alterar o peso, a meta de hidratação é atualizada automaticamente.",12,MUTED,false),lp(-1,-2,6));
        EditText input=new EditText(this);
        input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Novo peso em kg");input.setText(PlanData.fmt(PlanData.weight(this)).replace(",","."));
        c.addView(input,lp(-1,46,7));
        Button update=button("Atualizar peso e meta de água",true);
        c.addView(update,lp(-1,47,9));
        update.setOnClickListener(v->{
            try {
                float n=Float.parseFloat(input.getText().toString().replace(",",".").trim());
                if(n<30||n>350)throw new IllegalArgumentException();
                PlanData.setWeight(this,n);ReminderScheduler.setup(this);render();
                Toast.makeText(this,"Meta recalculada: "+PlanData.goal(this)+" ml/dia",Toast.LENGTH_LONG).show();
            } catch(Exception e){Toast.makeText(this,"Informe um peso válido entre 30 e 350 kg.",Toast.LENGTH_LONG).show();}
        });
        c.addView(text("Lembretes: refeições às 07h, 12h, 16h, 19h; água das 07h às 18h. Permita notificações e, se necessário, desative a otimização de bateria para este app.",12,MUTED,false),lp(-1,-2,13));
    }
}
