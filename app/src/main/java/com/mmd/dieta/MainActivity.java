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
        PlanData.start(this);chosen=PlanData.today(this);
        ReminderScheduler.setup(this);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
        render();
    }
    @Override protected void onResume(){super.onResume();if(root!=null)render();}
    private void render() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=vertical();root.setPadding(dp(17),dp(22),dp(17),dp(30));scroll.addView(root);setContentView(scroll);
        header();
        calendar();
        menu();
        hydration();
        weightCard();
        LinearLayout note=card("OBSERVAÇÕES");
        note.addView(text("Calorias e hidratação são estimativas. Pese arroz, batata e carne depois do preparo; aveia, farinha e Milharina antes do preparo. A meta de água não trata retenção de líquidos.",12,MUTED,false));
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
        int type=PlanData.type(chosen),total=PlanData.dayTotal(chosen);
        LinearLayout c=card("CARDÁPIO "+(char)('A'+type)+"  •  ~"+total+" kcal");
        c.addView(text("Início às 07:00  |  Pesos indicados em gramas",12,MUTED,false));
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
            TextView kcal=text("~"+PlanData.KCAL[type][m]+" kcal",12,BLUE,true);
            line.addView(kcal);
            TextView content=text(PlanData.FOOD[type][m],13,MUTED,false);
            sec.addView(content,lp(-1,-2,8));
            boolean done=PlanData.mealDone(this,chosen,m);
            Button confirm=button(done?"✓  Refeição concluída":"Confirmar refeição",done);
            sec.addView(confirm,lp(-1,42,10));
            confirm.setOnClickListener(v->{
                boolean newDone=!PlanData.mealDone(this,chosen,index);
                PlanData.setMeal(this,chosen,index,newDone);
                if(newDone)ReminderScheduler.cancelMeal(this,chosen,index);
                else ReminderScheduler.setup(this);
                render();
            });
        }
    }
    private void hydration() {
        int amount=PlanData.water(this,chosen),goal=PlanData.goal(this);
        LinearLayout c=card("HIDRATAÇÃO  💧");
        c.addView(text(amount+" / "+goal+" ml",26,WHITE,true));
        c.addView(text("Meta ajustável conforme seu peso • referência 30 ml/kg",12,MUTED,false));
        progress(c,amount,goal);
        LinearLayout buttons=new LinearLayout(this);
        c.addView(buttons,lp(-1,44,14));
        int[] values={200,300,-200};
        String[] labels={"+200 ml","+300 ml","Corrigir −200"};
        for(int i=0;i<3;i++){
            final int ml=values[i];Button b=button(labels[i],i==1);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            if(i>0)p.leftMargin=dp(5);
            buttons.addView(b,p);
            b.setOnClickListener(v->{PlanData.addWater(this,chosen,ml);render();});
        }
        gap(c,13);
        c.addView(text("HORÁRIOS SUGERIDOS",12,BLUE,true));
        int portion=Math.round(goal/110f)*10;
        c.addView(text("Aprox. "+portion+" ml por lembrete. Não é necessário beber tudo de uma vez.",12,MUTED,false));
        for(int k=0;k<PlanData.WATER_TIMES.length;k+=3) {
            LinearLayout line=new LinearLayout(this);c.addView(line,lp(-1,35,7));
            for(int j=k;j<Math.min(k+3,PlanData.WATER_TIMES.length);j++){
                String t=PlanData.WATER_TIMES[j];
                TextView chip=text("◉ "+t,12,BLUE,true);
                chip.setGravity(Gravity.CENTER);
                chip.setBackground(drawable(0xff102b44,LINE,9));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
                if(j>k)p.leftMargin=dp(7);
                line.addView(chip,p);
            }
        }
        c.addView(text("Os avisos de água param quando a meta do dia é atingida.",11,MUTED,false),lp(-1,-2,12));
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
        c.addView(text("Lembretes: refeições às 07h, 12h, 16h, 19h; água das 06h30 às 19h50. Permita notificações e, se necessário, desative a otimização de bateria para este app.",12,MUTED,false),lp(-1,-2,13));
    }
}
