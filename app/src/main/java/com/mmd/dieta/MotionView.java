package com.mmd.dieta;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;

/** Original offline vector animations. Illustrative, not a substitute for coaching. */
final class MotionView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String label="",kind="";
    private boolean animate=true;
    private long since=SystemClock.uptimeMillis();
    private static final int CYAN=0xff3de8f4,INK=0xffd8eafa,FADE=0xff567da5,GEAR=0xff627890;
    MotionView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void show(String name,boolean moving){
        if(!label.equals(name)){label=name;kind=name.toLowerCase(Locale.ROOT);since=SystemClock.uptimeMillis();setContentDescription("Ilustração de "+name);}
        animate=moving;invalidate();
    }
    void line(Canvas c,float x,float y,float xx,float yy,int color,float width){p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);c.drawLine(x,y,xx,yy,p);}
    void dot(Canvas c,float x,float y,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
    void rect(Canvas c,float x,float y,float xx,float yy,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,xx,yy,4,4,p);}
    void limb(Canvas c,float x,float y,float mx,float my,float ex,float ey,boolean near){int col=near?CYAN:FADE;line(c,x,y,mx,my,col,8);line(c,mx,my,ex,ey,col,7);dot(c,mx,my,4,INK);}
    void head(Canvas c,float x,float y){dot(c,x,y,13,INK);line(c,x,y+12,x,y+19,INK,6);}
    void body(Canvas c,float hx,float hy,float sx,float sy){line(c,hx,hy,sx,sy,INK,14);dot(c,hx,hy,7,CYAN);}
    void weight(Canvas c,float x,float y){line(c,x-13,y,x+13,y,GEAR,4);rect(c,x-15,y-7,x-9,y+7,INK);rect(c,x+9,y-7,x+15,y+7,INK);}
    void bar(Canvas c,float x,float y,float length){line(c,x-length/2,y,x+length/2,y,GEAR,4);rect(c,x-length/2-5,y-12,x-length/2+4,y+12,INK);rect(c,x+length/2-4,y-12,x+length/2+5,y+12,INK);}
    void bench(Canvas c,float x,float y,float xx,float yy){line(c,x,y,xx,yy,GEAR,9);line(c,x+9,y,x+9,151,GEAR,4);line(c,xx-9,yy,xx-9,151,GEAR,4);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();if(w==0||h==0)return;
        p.setStyle(Paint.Style.FILL);p.setColor(0xff0a2033);c.drawRoundRect(0,0,w,h,18,18,p);
        float scale=Math.min(w/300f,h/190f);c.save();c.translate((w-300*scale)/2,(h-190*scale)/2);c.scale(scale,scale);
        float t=animate?(SystemClock.uptimeMillis()-since)/1000f:0f;
        float z=(float)(.5-.5*Math.cos(t*Math.PI));float swing=(float)Math.sin(t*5);
        line(c,36,153,264,153,0xff29445c,2);
        if(kind.startsWith("aq")||kind.startsWith("cam")||kind.startsWith("st")||kind.startsWith("runl")||kind.contains("desafio")){
            boolean run=kind.startsWith("runl")||kind.contains("desafio");float a=run?(float)Math.sin(t*9):swing;
            float hip=92-(run?5:2)*Math.abs(a),sx=run?158:150;
            for(int i=0;i<5;i++){float x=45+((i*50-t*(run?55:25))%250+250)%250;line(c,x,160,x+12,160,0xff35516b,2);}
            limb(c,150,hip,150-a*24,121,150-a*39,150,false);
            limb(c,sx,56,sx+a*18,76,sx+a*29,62,false);
            body(c,150,hip,sx,55);head(c,sx+2,35);
            limb(c,150,hip,150+a*24,121-(run?12:4)*Math.max(0,a),150+a*39,150-(run?12:3)*Math.max(0,a),true);
            limb(c,sx,56,sx-a*18,75,sx-a*29,62,true);
        }else if(kind.contains("descanso")){
            bench(c,110,105,190,105);head(c,150,35);body(c,150,103,150,58);
            limb(c,150,103,180,106,184,150,true);limb(c,150,103,120,106,116,150,false);
            limb(c,150,59,125,83,120,104,true);limb(c,150,59,175,83,180,104,false);
        }else if(kind.contains("abdomina")){
            boolean second=kind.contains("bi-set")&&((int)(t/5)%2==1);bench(c,75,121,220,121);
            if(second){body(c,157,114,107,114);head(c,88,108);limb(c,157,114,185,91-z*28,211-z*25,108-z*58,true);limb(c,157,114,180,97-z*28,203-z*25,115-z*58,false);limb(c,107,114,120,130,154,130,true);}
            else {float sx=107+29*z,sy=108-38*z;limb(c,157,115,192,91,218,120,true);body(c,157,115,sx,sy);head(c,sx-13,sy-12);limb(c,sx,sy,sx+12,sy-17,sx+25,sy-3,true);weight(c,sx+25,sy-3);}
        }else if(kind.contains("supino")||kind.contains("crucifixo 45")){
            boolean incline=kind.contains("45");float sx=incline?105:103,sy=incline?80:112;
            bench(c,70,sy+10,188,122);body(c,177,114,sx,sy);head(c,sx-18,sy-7);limb(c,177,114,210,119,213,151,true);
            float ey=sy-24-42*z,ex=sx+16;
            if(kind.contains("crucifixo")){ex=sx-27+27*z;limb(c,sx,sy,sx-15-16*(1-z),sy-19,ex,ey,true);weight(c,ex,ey);}
            else {limb(c,sx,sy,sx+28*(1-z),sy-20-23*z,ex,ey,true);if(kind.contains("halter"))weight(c,ex,ey);else bar(c,ex,ey,80);}
        }else if(kind.contains("leg 45")||kind.contains("hack")||kind.contains("banco flexor")||kind.contains("extensor")||kind.contains("mesa flexora")||kind.contains("adutor")||kind.contains("abdutor")||kind.contains("panturrilha")){
            if(kind.contains("panturrilha")){boolean seated=kind.contains("sentado");float y=150-12*z;rect(c,110,151,196,157,GEAR);if(seated){bench(c,100,104,165,104);head(c,130,40);body(c,130,100,130,60);limb(c,130,100,182,104,183,y,true);weight(c,181,100);}else{head(c,150,30-12*z);body(c,150,98-12*z,150,52-12*z);limb(c,150,98-12*z,154,125-12*z,159,y,true);limb(c,150,98-12*z,142,125-12*z,146,y,false);limb(c,150,54-12*z,172,76-12*z,172,99-12*z,true);weight(c,172,99-12*z);} }
            else if(kind.contains("adutor")||kind.contains("abdutor")){bench(c,115,105,185,105);head(c,150,35);body(c,150,100,150,58);boolean ab=((int)(t/5)%2==0);float spread=ab?35-23*z:12+23*z;limb(c,150,100,150-spread,122,150-spread-5,150,true);limb(c,150,100,150+spread,122,150+spread+5,150,false);line(c,110,120,110,147,GEAR,4);line(c,190,120,190,147,GEAR,4);}
            else if(kind.contains("leg 45")){bench(c,70,109,165,141);line(c,186,68,253,130,GEAR,5);float x=210+20*z,y=89-20*z;line(c,x-15,y+14,x+15,y-14,INK,8);head(c,85,83);body(c,151,130,105,96);limb(c,151,130,179+16*z,95+12*z,x,y,true);}
            else if(kind.contains("hack")){float hy=98+22*(1-z);line(c,100,40,185,151,GEAR,8);head(c,140,hy-66);body(c,156,hy,148,hy-44);limb(c,156,hy,190-17*z,125,188,151,true);limb(c,148,hy-44,174,hy-28,168,hy-47,true);}
            else if(kind.contains("mesa flexora")){bench(c,72,117,213,117);head(c,83,100);body(c,155,111,107,111);limb(c,155,111,197,111,197+34*(1-z),111-38*z,true);dot(c,197+34*(1-z),111-38*z,8,GEAR);}
            else{bench(c,98,106,163,106);head(c,126,37);body(c,139,103,129,59);float x=kind.contains("extensor")?177+38*z:196-34*z,y=kind.contains("extensor")?149-41*z:119+30*z;limb(c,139,103,177,108,x,y,true);dot(c,x,y,8,GEAR);}
        }else if(kind.contains("puxador frontal")||kind.contains("remada baixa")||kind.contains("remada unilateral")||kind.contains("crucifixo invertido")||kind.contains("cross over")){
            if(kind.contains("puxador frontal")){line(c,210,20,210,150,GEAR,5);bench(c,112,111,177,111);head(c,147,55);body(c,147,106,147,76);limb(c,147,76,170,58+26*z,157,30+42*z,true);bar(c,147,30+42*z,kind.contains("aberta")?85:36);line(c,147,18,147,30+42*z,GEAR,2);limb(c,147,106,181,117,181,151,true);}
            else if(kind.contains("remada baixa")){bench(c,70,118,150,118);head(c,107,55);body(c,119,111,112,77);limb(c,119,111,170,108,214,144,true);float x=202-55*z;limb(c,112,77,146-12*z,92,x,93,true);line(c,x,93,242,93,GEAR,3);line(c,242,55,242,151,GEAR,6);}
            else if(kind.contains("remada unilateral")||kind.contains("crucifixo invertido")){bench(c,68,109,125,109);head(c,91,55);body(c,172,87,113,71);limb(c,172,87,184,121,189,151,true);float x=kind.contains("unilateral")?147:125+30*z;float y=kind.contains("unilateral")?132-55*z:129-66*z;limb(c,113,71,132,94-22*z,x,y,true);weight(c,x,y);limb(c,113,71,98,91,96,108,false);}
            else{line(c,48,25,48,148,GEAR,5);line(c,252,25,252,148,GEAR,5);head(c,150,34);body(c,150,98,150,55);limb(c,150,98,135,124,130,150,true);limb(c,150,98,165,124,170,150,false);float x=70+70*z;limb(c,150,55,110+17*z,69,x,83+15*z,true);limb(c,150,55,190-17*z,69,300-x,83+15*z,false);line(c,48,40,x,83+15*z,GEAR,2);line(c,252,40,300-x,83+15*z,GEAR,2);}
        }else {
            float sy=55,hy=98,hx=150,sx=150;
            boolean bend=kind.contains("bom dia");
            if(bend){sx=150-47*z;sy=55+37*z;}
            if(kind.contains("encolhimento"))sy-=10*z;
            head(c,sx,sy-23);body(c,hx,hy,sx,sy);limb(c,hx,hy,136,125,135,151,true);limb(c,hx,hy,164,125,165,151,false);
            float x=182,y=104,mx=175,my=80;
            if(kind.contains("rosca")){x=182-10*z;y=106-50*z;mx=173;my=86;if(kind.contains("scott"))bench(c,167,80,207,102);}
            else if(kind.contains("desenvolvimento")){x=182;y=59-38*z;mx=194-13*z;my=72-27*z;}
            else if(kind.contains("abdução")||kind.contains("abducao")){x=177+54*z;y=106-52*z;mx=167+28*z;my=81-26*z;}
            else if(kind.contains("remada alta")){x=165;y=105-51*z;mx=180+17*z;my=86-34*z;}
            else if(kind.contains("francês")||kind.contains("frances")){x=165+13*(1-z);y=46-29*z;mx=171;my=39;}
            else if(kind.contains("tríceps")||kind.contains("triceps")){x=182;y=72+36*z;mx=172;my=81;}
            else if(bend){x=sx+17;y=sy;mx=sx+25;my=sy+19;}
            else if(kind.contains("encolhimento")){x=182;y=104-10*z;my=80-10*z;}
            limb(c,sx,sy,mx,my,x,y,true);limb(c,sx,sy,2*sx-mx,my,2*sx-x,y,false);
            if(kind.contains("cabo")||kind.contains("polia")||kind.contains("puxador")){line(c,240,20,240,151,GEAR,5);line(c,240,kind.contains("tríceps puxador")?20:150,x,y,GEAR,2);dot(c,x,y,5,INK);}
            else if(kind.contains("barra")||kind.contains("remada alta")||bend)bar(c,sx,y,75);
            else{weight(c,x,y);weight(c,2*sx-x,y);}
        }
        p.setStyle(Paint.Style.FILL);p.setColor(0xff8eaac2);p.setTextSize(10);p.setTextAlign(Paint.Align.CENTER);c.drawText("DEMONSTRAÇÃO ESQUEMÁTICA",150,181,p);c.restore();
        if(animate&&isShown())postInvalidateDelayed(33);
    }
}