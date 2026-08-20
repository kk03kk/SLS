/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.blue.Aggregate;
import com.megacrit.cardcrawl.cards.blue.AllForOne;
import com.megacrit.cardcrawl.cards.blue.Amplify;
import com.megacrit.cardcrawl.cards.blue.AutoShields;
import com.megacrit.cardcrawl.cards.blue.BallLightning;
import com.megacrit.cardcrawl.cards.blue.Barrage;
import com.megacrit.cardcrawl.cards.blue.BeamCell;
import com.megacrit.cardcrawl.cards.blue.BiasedCognition;
import com.megacrit.cardcrawl.cards.blue.Blizzard;
import com.megacrit.cardcrawl.cards.blue.BootSequence;
import com.megacrit.cardcrawl.cards.blue.Buffer;
import com.megacrit.cardcrawl.cards.blue.Capacitor;
import com.megacrit.cardcrawl.cards.blue.Chaos;
import com.megacrit.cardcrawl.cards.blue.Chill;
import com.megacrit.cardcrawl.cards.blue.Claw;
import com.megacrit.cardcrawl.cards.blue.ColdSnap;
import com.megacrit.cardcrawl.cards.blue.CompileDriver;
import com.megacrit.cardcrawl.cards.blue.ConserveBattery;
import com.megacrit.cardcrawl.cards.blue.Consume;
import com.megacrit.cardcrawl.cards.blue.Coolheaded;
import com.megacrit.cardcrawl.cards.blue.CoreSurge;
import com.megacrit.cardcrawl.cards.blue.CreativeAI;
import com.megacrit.cardcrawl.cards.blue.Darkness;
import com.megacrit.cardcrawl.cards.blue.Defend_Blue;
import com.megacrit.cardcrawl.cards.blue.Defragment;
import com.megacrit.cardcrawl.cards.blue.DoomAndGloom;
import com.megacrit.cardcrawl.cards.blue.DoubleEnergy;
import com.megacrit.cardcrawl.cards.blue.Dualcast;
import com.megacrit.cardcrawl.cards.blue.EchoForm;
import com.megacrit.cardcrawl.cards.blue.Electrodynamics;
import com.megacrit.cardcrawl.cards.blue.Equilibrium;
import com.megacrit.cardcrawl.cards.blue.FTL;
import com.megacrit.cardcrawl.cards.blue.Fission;
import com.megacrit.cardcrawl.cards.blue.ForceField;
import com.megacrit.cardcrawl.cards.blue.Fusion;
import com.megacrit.cardcrawl.cards.blue.GeneticAlgorithm;
import com.megacrit.cardcrawl.cards.blue.Glacier;
import com.megacrit.cardcrawl.cards.blue.GoForTheEyes;
import com.megacrit.cardcrawl.cards.blue.Heatsinks;
import com.megacrit.cardcrawl.cards.blue.HelloWorld;
import com.megacrit.cardcrawl.cards.blue.Hologram;
import com.megacrit.cardcrawl.cards.blue.Hyperbeam;
import com.megacrit.cardcrawl.cards.blue.Leap;
import com.megacrit.cardcrawl.cards.blue.LockOn;
import com.megacrit.cardcrawl.cards.blue.Loop;
import com.megacrit.cardcrawl.cards.blue.MachineLearning;
import com.megacrit.cardcrawl.cards.blue.Melter;
import com.megacrit.cardcrawl.cards.blue.MeteorStrike;
import com.megacrit.cardcrawl.cards.blue.MultiCast;
import com.megacrit.cardcrawl.cards.blue.Overclock;
import com.megacrit.cardcrawl.cards.blue.Rainbow;
import com.megacrit.cardcrawl.cards.blue.Reboot;
import com.megacrit.cardcrawl.cards.blue.Rebound;
import com.megacrit.cardcrawl.cards.blue.Recursion;
import com.megacrit.cardcrawl.cards.blue.Recycle;
import com.megacrit.cardcrawl.cards.blue.ReinforcedBody;
import com.megacrit.cardcrawl.cards.blue.Reprogram;
import com.megacrit.cardcrawl.cards.blue.RipAndTear;
import com.megacrit.cardcrawl.cards.blue.Scrape;
import com.megacrit.cardcrawl.cards.blue.Seek;
import com.megacrit.cardcrawl.cards.blue.SelfRepair;
import com.megacrit.cardcrawl.cards.blue.Skim;
import com.megacrit.cardcrawl.cards.blue.Stack;
import com.megacrit.cardcrawl.cards.blue.StaticDischarge;
import com.megacrit.cardcrawl.cards.blue.SteamBarrier;
import com.megacrit.cardcrawl.cards.blue.Storm;
import com.megacrit.cardcrawl.cards.blue.Streamline;
import com.megacrit.cardcrawl.cards.blue.Strike_Blue;
import com.megacrit.cardcrawl.cards.blue.Sunder;
import com.megacrit.cardcrawl.cards.blue.SweepingBeam;
import com.megacrit.cardcrawl.cards.blue.Tempest;
import com.megacrit.cardcrawl.cards.blue.ThunderStrike;
import com.megacrit.cardcrawl.cards.blue.Turbo;
import com.megacrit.cardcrawl.cards.blue.WhiteNoise;
import com.megacrit.cardcrawl.cards.blue.Zap;
import com.megacrit.cardcrawl.cards.colorless.Apotheosis;
import com.megacrit.cardcrawl.cards.colorless.Apparition;
import com.megacrit.cardcrawl.cards.colorless.BandageUp;
import com.megacrit.cardcrawl.cards.colorless.Bite;
import com.megacrit.cardcrawl.cards.colorless.Blind;
import com.megacrit.cardcrawl.cards.colorless.Chrysalis;
import com.megacrit.cardcrawl.cards.colorless.DarkShackles;
import com.megacrit.cardcrawl.cards.colorless.DeepBreath;
import com.megacrit.cardcrawl.cards.colorless.Discovery;
import com.megacrit.cardcrawl.cards.colorless.DramaticEntrance;
import com.megacrit.cardcrawl.cards.colorless.Enlightenment;
import com.megacrit.cardcrawl.cards.colorless.Finesse;
import com.megacrit.cardcrawl.cards.colorless.FlashOfSteel;
import com.megacrit.cardcrawl.cards.colorless.Forethought;
import com.megacrit.cardcrawl.cards.colorless.GoodInstincts;
import com.megacrit.cardcrawl.cards.colorless.HandOfGreed;
import com.megacrit.cardcrawl.cards.colorless.Impatience;
import com.megacrit.cardcrawl.cards.colorless.JAX;
import com.megacrit.cardcrawl.cards.colorless.JackOfAllTrades;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.cards.colorless.Magnetism;
import com.megacrit.cardcrawl.cards.colorless.MasterOfStrategy;
import com.megacrit.cardcrawl.cards.colorless.Mayhem;
import com.megacrit.cardcrawl.cards.colorless.Metamorphosis;
import com.megacrit.cardcrawl.cards.colorless.MindBlast;
import com.megacrit.cardcrawl.cards.colorless.Panacea;
import com.megacrit.cardcrawl.cards.colorless.Panache;
import com.megacrit.cardcrawl.cards.colorless.PanicButton;
import com.megacrit.cardcrawl.cards.colorless.Purity;
import com.megacrit.cardcrawl.cards.colorless.RitualDagger;
import com.megacrit.cardcrawl.cards.colorless.SadisticNature;
import com.megacrit.cardcrawl.cards.colorless.SecretTechnique;
import com.megacrit.cardcrawl.cards.colorless.SecretWeapon;
import com.megacrit.cardcrawl.cards.colorless.SwiftStrike;
import com.megacrit.cardcrawl.cards.colorless.TheBomb;
import com.megacrit.cardcrawl.cards.colorless.ThinkingAhead;
import com.megacrit.cardcrawl.cards.colorless.Transmutation;
import com.megacrit.cardcrawl.cards.colorless.Trip;
import com.megacrit.cardcrawl.cards.colorless.Violence;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.cards.curses.Clumsy;
import com.megacrit.cardcrawl.cards.curses.CurseOfTheBell;
import com.megacrit.cardcrawl.cards.curses.Decay;
import com.megacrit.cardcrawl.cards.curses.Doubt;
import com.megacrit.cardcrawl.cards.curses.Injury;
import com.megacrit.cardcrawl.cards.curses.Necronomicurse;
import com.megacrit.cardcrawl.cards.curses.Normality;
import com.megacrit.cardcrawl.cards.curses.Pain;
import com.megacrit.cardcrawl.cards.curses.Parasite;
import com.megacrit.cardcrawl.cards.curses.Pride;
import com.megacrit.cardcrawl.cards.curses.Regret;
import com.megacrit.cardcrawl.cards.curses.Shame;
import com.megacrit.cardcrawl.cards.curses.Writhe;
import com.megacrit.cardcrawl.cards.green.AThousandCuts;
import com.megacrit.cardcrawl.cards.green.Accuracy;
import com.megacrit.cardcrawl.cards.green.Acrobatics;
import com.megacrit.cardcrawl.cards.green.Adrenaline;
import com.megacrit.cardcrawl.cards.green.AfterImage;
import com.megacrit.cardcrawl.cards.green.Alchemize;
import com.megacrit.cardcrawl.cards.green.AllOutAttack;
import com.megacrit.cardcrawl.cards.green.Backflip;
import com.megacrit.cardcrawl.cards.green.Backstab;
import com.megacrit.cardcrawl.cards.green.Bane;
import com.megacrit.cardcrawl.cards.green.BladeDance;
import com.megacrit.cardcrawl.cards.green.Blur;
import com.megacrit.cardcrawl.cards.green.BouncingFlask;
import com.megacrit.cardcrawl.cards.green.BulletTime;
import com.megacrit.cardcrawl.cards.green.Burst;
import com.megacrit.cardcrawl.cards.green.CalculatedGamble;
import com.megacrit.cardcrawl.cards.green.Caltrops;
import com.megacrit.cardcrawl.cards.green.Catalyst;
import com.megacrit.cardcrawl.cards.green.Choke;
import com.megacrit.cardcrawl.cards.green.CloakAndDagger;
import com.megacrit.cardcrawl.cards.green.Concentrate;
import com.megacrit.cardcrawl.cards.green.CorpseExplosion;
import com.megacrit.cardcrawl.cards.green.CripplingPoison;
import com.megacrit.cardcrawl.cards.green.DaggerSpray;
import com.megacrit.cardcrawl.cards.green.DaggerThrow;
import com.megacrit.cardcrawl.cards.green.Dash;
import com.megacrit.cardcrawl.cards.green.DeadlyPoison;
import com.megacrit.cardcrawl.cards.green.Defend_Green;
import com.megacrit.cardcrawl.cards.green.Deflect;
import com.megacrit.cardcrawl.cards.green.DieDieDie;
import com.megacrit.cardcrawl.cards.green.Distraction;
import com.megacrit.cardcrawl.cards.green.DodgeAndRoll;
import com.megacrit.cardcrawl.cards.green.Doppelganger;
import com.megacrit.cardcrawl.cards.green.EndlessAgony;
import com.megacrit.cardcrawl.cards.green.Envenom;
import com.megacrit.cardcrawl.cards.green.EscapePlan;
import com.megacrit.cardcrawl.cards.green.Eviscerate;
import com.megacrit.cardcrawl.cards.green.Expertise;
import com.megacrit.cardcrawl.cards.green.Finisher;
import com.megacrit.cardcrawl.cards.green.Flechettes;
import com.megacrit.cardcrawl.cards.green.FlyingKnee;
import com.megacrit.cardcrawl.cards.green.Footwork;
import com.megacrit.cardcrawl.cards.green.GlassKnife;
import com.megacrit.cardcrawl.cards.green.GrandFinale;
import com.megacrit.cardcrawl.cards.green.HeelHook;
import com.megacrit.cardcrawl.cards.green.InfiniteBlades;
import com.megacrit.cardcrawl.cards.green.LegSweep;
import com.megacrit.cardcrawl.cards.green.Malaise;
import com.megacrit.cardcrawl.cards.green.MasterfulStab;
import com.megacrit.cardcrawl.cards.green.Neutralize;
import com.megacrit.cardcrawl.cards.green.Nightmare;
import com.megacrit.cardcrawl.cards.green.NoxiousFumes;
import com.megacrit.cardcrawl.cards.green.Outmaneuver;
import com.megacrit.cardcrawl.cards.green.PhantasmalKiller;
import com.megacrit.cardcrawl.cards.green.PiercingWail;
import com.megacrit.cardcrawl.cards.green.PoisonedStab;
import com.megacrit.cardcrawl.cards.green.Predator;
import com.megacrit.cardcrawl.cards.green.Prepared;
import com.megacrit.cardcrawl.cards.green.QuickSlash;
import com.megacrit.cardcrawl.cards.green.Reflex;
import com.megacrit.cardcrawl.cards.green.RiddleWithHoles;
import com.megacrit.cardcrawl.cards.green.Setup;
import com.megacrit.cardcrawl.cards.green.Skewer;
import com.megacrit.cardcrawl.cards.green.Slice;
import com.megacrit.cardcrawl.cards.green.SneakyStrike;
import com.megacrit.cardcrawl.cards.green.StormOfSteel;
import com.megacrit.cardcrawl.cards.green.Strike_Green;
import com.megacrit.cardcrawl.cards.green.SuckerPunch;
import com.megacrit.cardcrawl.cards.green.Survivor;
import com.megacrit.cardcrawl.cards.green.Tactician;
import com.megacrit.cardcrawl.cards.green.Terror;
import com.megacrit.cardcrawl.cards.green.ToolsOfTheTrade;
import com.megacrit.cardcrawl.cards.green.Unload;
import com.megacrit.cardcrawl.cards.green.WellLaidPlans;
import com.megacrit.cardcrawl.cards.green.WraithForm;
import com.megacrit.cardcrawl.cards.optionCards.BecomeAlmighty;
import com.megacrit.cardcrawl.cards.optionCards.FameAndFortune;
import com.megacrit.cardcrawl.cards.optionCards.LiveForever;
import com.megacrit.cardcrawl.cards.purple.Alpha;
import com.megacrit.cardcrawl.cards.purple.BattleHymn;
import com.megacrit.cardcrawl.cards.purple.Blasphemy;
import com.megacrit.cardcrawl.cards.purple.BowlingBash;
import com.megacrit.cardcrawl.cards.purple.Brilliance;
import com.megacrit.cardcrawl.cards.purple.CarveReality;
import com.megacrit.cardcrawl.cards.purple.Collect;
import com.megacrit.cardcrawl.cards.purple.Conclude;
import com.megacrit.cardcrawl.cards.purple.ConjureBlade;
import com.megacrit.cardcrawl.cards.purple.Consecrate;
import com.megacrit.cardcrawl.cards.purple.Crescendo;
import com.megacrit.cardcrawl.cards.purple.CrushJoints;
import com.megacrit.cardcrawl.cards.purple.CutThroughFate;
import com.megacrit.cardcrawl.cards.purple.DeceiveReality;
import com.megacrit.cardcrawl.cards.purple.Defend_Watcher;
import com.megacrit.cardcrawl.cards.purple.DeusExMachina;
import com.megacrit.cardcrawl.cards.purple.DevaForm;
import com.megacrit.cardcrawl.cards.purple.Devotion;
import com.megacrit.cardcrawl.cards.purple.EmptyBody;
import com.megacrit.cardcrawl.cards.purple.EmptyFist;
import com.megacrit.cardcrawl.cards.purple.EmptyMind;
import com.megacrit.cardcrawl.cards.purple.Eruption;
import com.megacrit.cardcrawl.cards.purple.Establishment;
import com.megacrit.cardcrawl.cards.purple.Evaluate;
import com.megacrit.cardcrawl.cards.purple.Fasting;
import com.megacrit.cardcrawl.cards.purple.FearNoEvil;
import com.megacrit.cardcrawl.cards.purple.FlurryOfBlows;
import com.megacrit.cardcrawl.cards.purple.FlyingSleeves;
import com.megacrit.cardcrawl.cards.purple.FollowUp;
import com.megacrit.cardcrawl.cards.purple.ForeignInfluence;
import com.megacrit.cardcrawl.cards.purple.Foresight;
import com.megacrit.cardcrawl.cards.purple.Halt;
import com.megacrit.cardcrawl.cards.purple.Indignation;
import com.megacrit.cardcrawl.cards.purple.InnerPeace;
import com.megacrit.cardcrawl.cards.purple.Judgement;
import com.megacrit.cardcrawl.cards.purple.JustLucky;
import com.megacrit.cardcrawl.cards.purple.LessonLearned;
import com.megacrit.cardcrawl.cards.purple.LikeWater;
import com.megacrit.cardcrawl.cards.purple.MasterReality;
import com.megacrit.cardcrawl.cards.purple.Meditate;
import com.megacrit.cardcrawl.cards.purple.MentalFortress;
import com.megacrit.cardcrawl.cards.purple.Nirvana;
import com.megacrit.cardcrawl.cards.purple.Omniscience;
import com.megacrit.cardcrawl.cards.purple.Perseverance;
import com.megacrit.cardcrawl.cards.purple.Pray;
import com.megacrit.cardcrawl.cards.purple.PressurePoints;
import com.megacrit.cardcrawl.cards.purple.Prostrate;
import com.megacrit.cardcrawl.cards.purple.Protect;
import com.megacrit.cardcrawl.cards.purple.Ragnarok;
import com.megacrit.cardcrawl.cards.purple.ReachHeaven;
import com.megacrit.cardcrawl.cards.purple.Rushdown;
import com.megacrit.cardcrawl.cards.purple.Sanctity;
import com.megacrit.cardcrawl.cards.purple.SandsOfTime;
import com.megacrit.cardcrawl.cards.purple.SashWhip;
import com.megacrit.cardcrawl.cards.purple.Scrawl;
import com.megacrit.cardcrawl.cards.purple.SignatureMove;
import com.megacrit.cardcrawl.cards.purple.SimmeringFury;
import com.megacrit.cardcrawl.cards.purple.SpiritShield;
import com.megacrit.cardcrawl.cards.purple.Strike_Purple;
import com.megacrit.cardcrawl.cards.purple.Study;
import com.megacrit.cardcrawl.cards.purple.Swivel;
import com.megacrit.cardcrawl.cards.purple.TalkToTheHand;
import com.megacrit.cardcrawl.cards.purple.Tantrum;
import com.megacrit.cardcrawl.cards.purple.ThirdEye;
import com.megacrit.cardcrawl.cards.purple.Tranquility;
import com.megacrit.cardcrawl.cards.purple.Vault;
import com.megacrit.cardcrawl.cards.purple.Vigilance;
import com.megacrit.cardcrawl.cards.purple.Wallop;
import com.megacrit.cardcrawl.cards.purple.WaveOfTheHand;
import com.megacrit.cardcrawl.cards.purple.Weave;
import com.megacrit.cardcrawl.cards.purple.WheelKick;
import com.megacrit.cardcrawl.cards.purple.WindmillStrike;
import com.megacrit.cardcrawl.cards.purple.Wish;
import com.megacrit.cardcrawl.cards.purple.Worship;
import com.megacrit.cardcrawl.cards.purple.WreathOfFlame;
import com.megacrit.cardcrawl.cards.red.Anger;
import com.megacrit.cardcrawl.cards.red.Armaments;
import com.megacrit.cardcrawl.cards.red.Barricade;
import com.megacrit.cardcrawl.cards.red.Bash;
import com.megacrit.cardcrawl.cards.red.BattleTrance;
import com.megacrit.cardcrawl.cards.red.Berserk;
import com.megacrit.cardcrawl.cards.red.BloodForBlood;
import com.megacrit.cardcrawl.cards.red.Bloodletting;
import com.megacrit.cardcrawl.cards.red.Bludgeon;
import com.megacrit.cardcrawl.cards.red.BodySlam;
import com.megacrit.cardcrawl.cards.red.Brutality;
import com.megacrit.cardcrawl.cards.red.BurningPact;
import com.megacrit.cardcrawl.cards.red.Carnage;
import com.megacrit.cardcrawl.cards.red.Clash;
import com.megacrit.cardcrawl.cards.red.Cleave;
import com.megacrit.cardcrawl.cards.red.Clothesline;
import com.megacrit.cardcrawl.cards.red.Combust;
import com.megacrit.cardcrawl.cards.red.Corruption;
import com.megacrit.cardcrawl.cards.red.DarkEmbrace;
import com.megacrit.cardcrawl.cards.red.Defend_Red;
import com.megacrit.cardcrawl.cards.red.DemonForm;
import com.megacrit.cardcrawl.cards.red.Disarm;
import com.megacrit.cardcrawl.cards.red.DoubleTap;
import com.megacrit.cardcrawl.cards.red.Dropkick;
import com.megacrit.cardcrawl.cards.red.DualWield;
import com.megacrit.cardcrawl.cards.red.Entrench;
import com.megacrit.cardcrawl.cards.red.Evolve;
import com.megacrit.cardcrawl.cards.red.Exhume;
import com.megacrit.cardcrawl.cards.red.Feed;
import com.megacrit.cardcrawl.cards.red.FeelNoPain;
import com.megacrit.cardcrawl.cards.red.FiendFire;
import com.megacrit.cardcrawl.cards.red.FireBreathing;
import com.megacrit.cardcrawl.cards.red.FlameBarrier;
import com.megacrit.cardcrawl.cards.red.Flex;
import com.megacrit.cardcrawl.cards.red.GhostlyArmor;
import com.megacrit.cardcrawl.cards.red.Havoc;
import com.megacrit.cardcrawl.cards.red.Headbutt;
import com.megacrit.cardcrawl.cards.red.HeavyBlade;
import com.megacrit.cardcrawl.cards.red.Hemokinesis;
import com.megacrit.cardcrawl.cards.red.Immolate;
import com.megacrit.cardcrawl.cards.red.Impervious;
import com.megacrit.cardcrawl.cards.red.InfernalBlade;
import com.megacrit.cardcrawl.cards.red.Inflame;
import com.megacrit.cardcrawl.cards.red.Intimidate;
import com.megacrit.cardcrawl.cards.red.IronWave;
import com.megacrit.cardcrawl.cards.red.Juggernaut;
import com.megacrit.cardcrawl.cards.red.LimitBreak;
import com.megacrit.cardcrawl.cards.red.Metallicize;
import com.megacrit.cardcrawl.cards.red.Offering;
import com.megacrit.cardcrawl.cards.red.PerfectedStrike;
import com.megacrit.cardcrawl.cards.red.PommelStrike;
import com.megacrit.cardcrawl.cards.red.PowerThrough;
import com.megacrit.cardcrawl.cards.red.Pummel;
import com.megacrit.cardcrawl.cards.red.Rage;
import com.megacrit.cardcrawl.cards.red.Rampage;
import com.megacrit.cardcrawl.cards.red.Reaper;
import com.megacrit.cardcrawl.cards.red.RecklessCharge;
import com.megacrit.cardcrawl.cards.red.Rupture;
import com.megacrit.cardcrawl.cards.red.SearingBlow;
import com.megacrit.cardcrawl.cards.red.SecondWind;
import com.megacrit.cardcrawl.cards.red.SeeingRed;
import com.megacrit.cardcrawl.cards.red.Sentinel;
import com.megacrit.cardcrawl.cards.red.SeverSoul;
import com.megacrit.cardcrawl.cards.red.Shockwave;
import com.megacrit.cardcrawl.cards.red.ShrugItOff;
import com.megacrit.cardcrawl.cards.red.SpotWeakness;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.cards.red.SwordBoomerang;
import com.megacrit.cardcrawl.cards.red.ThunderClap;
import com.megacrit.cardcrawl.cards.red.TrueGrit;
import com.megacrit.cardcrawl.cards.red.TwinStrike;
import com.megacrit.cardcrawl.cards.red.Uppercut;
import com.megacrit.cardcrawl.cards.red.Warcry;
import com.megacrit.cardcrawl.cards.red.Whirlwind;
import com.megacrit.cardcrawl.cards.red.WildStrike;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.cards.tempCards.Beta;
import com.megacrit.cardcrawl.cards.tempCards.Expunger;
import com.megacrit.cardcrawl.cards.tempCards.Insight;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.cards.tempCards.Omega;
import com.megacrit.cardcrawl.cards.tempCards.Safety;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.cards.tempCards.Smite;
import com.megacrit.cardcrawl.cards.tempCards.ThroughViolence;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CardLibrary {
    private static final Logger logger = LogManager.getLogger(CardLibrary.class.getName());
    public static int totalCardCount = 0;
    public static HashMap<String, AbstractCard> cards = new HashMap();
    private static HashMap<String, AbstractCard> curses = new HashMap();
    public static int redCards = 0;
    public static int greenCards = 0;
    public static int blueCards = 0;
    public static int purpleCards = 0;
    public static int colorlessCards = 0;
    public static int curseCards = 0;
    public static int seenRedCards = 0;
    public static int seenGreenCards = 0;
    public static int seenBlueCards = 0;
    public static int seenPurpleCards = 0;
    public static int seenColorlessCards = 0;
    public static int seenCurseCards = 0;

    public static void initialize() {
        long startTime = System.currentTimeMillis();
        CardLibrary.addRedCards();
        CardLibrary.addGreenCards();
        CardLibrary.addBlueCards();
        CardLibrary.addPurpleCards();
        CardLibrary.addColorlessCards();
        CardLibrary.addCurseCards();
        if (Settings.isDev) {
            // empty if block
        }
        logger.info("Card load time: " + (System.currentTimeMillis() - startTime) + "ms with " + cards.size() + " cards");
        if (Settings.isDev) {
            logger.info("[INFO] Red Cards: \t" + redCards);
            logger.info("[INFO] Green Cards: \t" + greenCards);
            logger.info("[INFO] Blue Cards: \t" + blueCards);
            logger.info("[INFO] Purple Cards: \t" + purpleCards);
            logger.info("[INFO] Colorless Cards: \t" + colorlessCards);
            logger.info("[INFO] Curse Cards: \t" + curseCards);
            logger.info("[INFO] Total Cards: \t" + (redCards + greenCards + blueCards + purpleCards + colorlessCards + curseCards));
        }
    }

    public static void resetForReload() {
        cards = new HashMap();
        curses = new HashMap();
        totalCardCount = 0;
        redCards = 0;
        greenCards = 0;
        blueCards = 0;
        purpleCards = 0;
        colorlessCards = 0;
        curseCards = 0;
        seenRedCards = 0;
        seenGreenCards = 0;
        seenBlueCards = 0;
        seenPurpleCards = 0;
        seenColorlessCards = 0;
        seenCurseCards = 0;
    }

    private static void addRedCards() {
        CardLibrary.add(new Anger());
        CardLibrary.add(new Armaments());
        CardLibrary.add(new Barricade());
        CardLibrary.add(new Bash());
        CardLibrary.add(new BattleTrance());
        CardLibrary.add(new Berserk());
        CardLibrary.add(new BloodForBlood());
        CardLibrary.add(new Bloodletting());
        CardLibrary.add(new Bludgeon());
        CardLibrary.add(new BodySlam());
        CardLibrary.add(new Brutality());
        CardLibrary.add(new BurningPact());
        CardLibrary.add(new Carnage());
        CardLibrary.add(new Clash());
        CardLibrary.add(new Cleave());
        CardLibrary.add(new Clothesline());
        CardLibrary.add(new Combust());
        CardLibrary.add(new Corruption());
        CardLibrary.add(new DarkEmbrace());
        CardLibrary.add(new Defend_Red());
        CardLibrary.add(new DemonForm());
        CardLibrary.add(new Disarm());
        CardLibrary.add(new DoubleTap());
        CardLibrary.add(new Dropkick());
        CardLibrary.add(new DualWield());
        CardLibrary.add(new Entrench());
        CardLibrary.add(new Evolve());
        CardLibrary.add(new Exhume());
        CardLibrary.add(new Feed());
        CardLibrary.add(new FeelNoPain());
        CardLibrary.add(new FiendFire());
        CardLibrary.add(new FireBreathing());
        CardLibrary.add(new FlameBarrier());
        CardLibrary.add(new Flex());
        CardLibrary.add(new GhostlyArmor());
        CardLibrary.add(new Havoc());
        CardLibrary.add(new Headbutt());
        CardLibrary.add(new HeavyBlade());
        CardLibrary.add(new Hemokinesis());
        CardLibrary.add(new Immolate());
        CardLibrary.add(new Impervious());
        CardLibrary.add(new InfernalBlade());
        CardLibrary.add(new Inflame());
        CardLibrary.add(new Intimidate());
        CardLibrary.add(new IronWave());
        CardLibrary.add(new Juggernaut());
        CardLibrary.add(new LimitBreak());
        CardLibrary.add(new Metallicize());
        CardLibrary.add(new Offering());
        CardLibrary.add(new PerfectedStrike());
        CardLibrary.add(new PommelStrike());
        CardLibrary.add(new PowerThrough());
        CardLibrary.add(new Pummel());
        CardLibrary.add(new Rage());
        CardLibrary.add(new Rampage());
        CardLibrary.add(new Reaper());
        CardLibrary.add(new RecklessCharge());
        CardLibrary.add(new Rupture());
        CardLibrary.add(new SearingBlow());
        CardLibrary.add(new SecondWind());
        CardLibrary.add(new SeeingRed());
        CardLibrary.add(new Sentinel());
        CardLibrary.add(new SeverSoul());
        CardLibrary.add(new Shockwave());
        CardLibrary.add(new ShrugItOff());
        CardLibrary.add(new SpotWeakness());
        CardLibrary.add(new Strike_Red());
        CardLibrary.add(new SwordBoomerang());
        CardLibrary.add(new ThunderClap());
        CardLibrary.add(new TrueGrit());
        CardLibrary.add(new TwinStrike());
        CardLibrary.add(new Uppercut());
        CardLibrary.add(new Warcry());
        CardLibrary.add(new Whirlwind());
        CardLibrary.add(new WildStrike());
    }

    private static void addGreenCards() {
        CardLibrary.add(new Accuracy());
        CardLibrary.add(new Acrobatics());
        CardLibrary.add(new Adrenaline());
        CardLibrary.add(new AfterImage());
        CardLibrary.add(new Alchemize());
        CardLibrary.add(new AllOutAttack());
        CardLibrary.add(new AThousandCuts());
        CardLibrary.add(new Backflip());
        CardLibrary.add(new Backstab());
        CardLibrary.add(new Bane());
        CardLibrary.add(new BladeDance());
        CardLibrary.add(new Blur());
        CardLibrary.add(new BouncingFlask());
        CardLibrary.add(new BulletTime());
        CardLibrary.add(new Burst());
        CardLibrary.add(new CalculatedGamble());
        CardLibrary.add(new Caltrops());
        CardLibrary.add(new Catalyst());
        CardLibrary.add(new Choke());
        CardLibrary.add(new CloakAndDagger());
        CardLibrary.add(new Concentrate());
        CardLibrary.add(new CorpseExplosion());
        CardLibrary.add(new CripplingPoison());
        CardLibrary.add(new DaggerSpray());
        CardLibrary.add(new DaggerThrow());
        CardLibrary.add(new Dash());
        CardLibrary.add(new DeadlyPoison());
        CardLibrary.add(new Defend_Green());
        CardLibrary.add(new Deflect());
        CardLibrary.add(new DieDieDie());
        CardLibrary.add(new Distraction());
        CardLibrary.add(new DodgeAndRoll());
        CardLibrary.add(new Doppelganger());
        CardLibrary.add(new EndlessAgony());
        CardLibrary.add(new Envenom());
        CardLibrary.add(new EscapePlan());
        CardLibrary.add(new Eviscerate());
        CardLibrary.add(new Expertise());
        CardLibrary.add(new Finisher());
        CardLibrary.add(new Flechettes());
        CardLibrary.add(new FlyingKnee());
        CardLibrary.add(new Footwork());
        CardLibrary.add(new GlassKnife());
        CardLibrary.add(new GrandFinale());
        CardLibrary.add(new HeelHook());
        CardLibrary.add(new InfiniteBlades());
        CardLibrary.add(new LegSweep());
        CardLibrary.add(new Malaise());
        CardLibrary.add(new MasterfulStab());
        CardLibrary.add(new Neutralize());
        CardLibrary.add(new Nightmare());
        CardLibrary.add(new NoxiousFumes());
        CardLibrary.add(new Outmaneuver());
        CardLibrary.add(new PhantasmalKiller());
        CardLibrary.add(new PiercingWail());
        CardLibrary.add(new PoisonedStab());
        CardLibrary.add(new Predator());
        CardLibrary.add(new Prepared());
        CardLibrary.add(new QuickSlash());
        CardLibrary.add(new Reflex());
        CardLibrary.add(new RiddleWithHoles());
        CardLibrary.add(new Setup());
        CardLibrary.add(new Skewer());
        CardLibrary.add(new Slice());
        CardLibrary.add(new StormOfSteel());
        CardLibrary.add(new Strike_Green());
        CardLibrary.add(new SuckerPunch());
        CardLibrary.add(new Survivor());
        CardLibrary.add(new Tactician());
        CardLibrary.add(new Terror());
        CardLibrary.add(new ToolsOfTheTrade());
        CardLibrary.add(new SneakyStrike());
        CardLibrary.add(new Unload());
        CardLibrary.add(new WellLaidPlans());
        CardLibrary.add(new WraithForm());
    }

    private static void addBlueCards() {
        CardLibrary.add(new Aggregate());
        CardLibrary.add(new AllForOne());
        CardLibrary.add(new Amplify());
        CardLibrary.add(new AutoShields());
        CardLibrary.add(new BallLightning());
        CardLibrary.add(new Barrage());
        CardLibrary.add(new BeamCell());
        CardLibrary.add(new BiasedCognition());
        CardLibrary.add(new Blizzard());
        CardLibrary.add(new BootSequence());
        CardLibrary.add(new Buffer());
        CardLibrary.add(new Capacitor());
        CardLibrary.add(new Chaos());
        CardLibrary.add(new Chill());
        CardLibrary.add(new Claw());
        CardLibrary.add(new ColdSnap());
        CardLibrary.add(new CompileDriver());
        CardLibrary.add(new ConserveBattery());
        CardLibrary.add(new Consume());
        CardLibrary.add(new Coolheaded());
        CardLibrary.add(new CoreSurge());
        CardLibrary.add(new CreativeAI());
        CardLibrary.add(new Darkness());
        CardLibrary.add(new Defend_Blue());
        CardLibrary.add(new Defragment());
        CardLibrary.add(new DoomAndGloom());
        CardLibrary.add(new DoubleEnergy());
        CardLibrary.add(new Dualcast());
        CardLibrary.add(new EchoForm());
        CardLibrary.add(new Electrodynamics());
        CardLibrary.add(new Fission());
        CardLibrary.add(new ForceField());
        CardLibrary.add(new FTL());
        CardLibrary.add(new Fusion());
        CardLibrary.add(new GeneticAlgorithm());
        CardLibrary.add(new Glacier());
        CardLibrary.add(new GoForTheEyes());
        CardLibrary.add(new Heatsinks());
        CardLibrary.add(new HelloWorld());
        CardLibrary.add(new Hologram());
        CardLibrary.add(new Hyperbeam());
        CardLibrary.add(new Leap());
        CardLibrary.add(new LockOn());
        CardLibrary.add(new Loop());
        CardLibrary.add(new MachineLearning());
        CardLibrary.add(new Melter());
        CardLibrary.add(new MeteorStrike());
        CardLibrary.add(new MultiCast());
        CardLibrary.add(new Overclock());
        CardLibrary.add(new Rainbow());
        CardLibrary.add(new Reboot());
        CardLibrary.add(new Rebound());
        CardLibrary.add(new Recursion());
        CardLibrary.add(new Recycle());
        CardLibrary.add(new ReinforcedBody());
        CardLibrary.add(new Reprogram());
        CardLibrary.add(new RipAndTear());
        CardLibrary.add(new Scrape());
        CardLibrary.add(new Seek());
        CardLibrary.add(new SelfRepair());
        CardLibrary.add(new Skim());
        CardLibrary.add(new Stack());
        CardLibrary.add(new StaticDischarge());
        CardLibrary.add(new SteamBarrier());
        CardLibrary.add(new Storm());
        CardLibrary.add(new Streamline());
        CardLibrary.add(new Strike_Blue());
        CardLibrary.add(new Sunder());
        CardLibrary.add(new SweepingBeam());
        CardLibrary.add(new Tempest());
        CardLibrary.add(new ThunderStrike());
        CardLibrary.add(new Turbo());
        CardLibrary.add(new Equilibrium());
        CardLibrary.add(new WhiteNoise());
        CardLibrary.add(new Zap());
    }

    private static void addPurpleCards() {
        CardLibrary.add(new Alpha());
        CardLibrary.add(new BattleHymn());
        CardLibrary.add(new Blasphemy());
        CardLibrary.add(new BowlingBash());
        CardLibrary.add(new Brilliance());
        CardLibrary.add(new CarveReality());
        CardLibrary.add(new Collect());
        CardLibrary.add(new Conclude());
        CardLibrary.add(new ConjureBlade());
        CardLibrary.add(new Consecrate());
        CardLibrary.add(new Crescendo());
        CardLibrary.add(new CrushJoints());
        CardLibrary.add(new CutThroughFate());
        CardLibrary.add(new DeceiveReality());
        CardLibrary.add(new Defend_Watcher());
        CardLibrary.add(new DeusExMachina());
        CardLibrary.add(new DevaForm());
        CardLibrary.add(new Devotion());
        CardLibrary.add(new EmptyBody());
        CardLibrary.add(new EmptyFist());
        CardLibrary.add(new EmptyMind());
        CardLibrary.add(new Eruption());
        CardLibrary.add(new Establishment());
        CardLibrary.add(new Evaluate());
        CardLibrary.add(new Fasting());
        CardLibrary.add(new FearNoEvil());
        CardLibrary.add(new FlurryOfBlows());
        CardLibrary.add(new FlyingSleeves());
        CardLibrary.add(new FollowUp());
        CardLibrary.add(new ForeignInfluence());
        CardLibrary.add(new Foresight());
        CardLibrary.add(new Halt());
        CardLibrary.add(new Indignation());
        CardLibrary.add(new InnerPeace());
        CardLibrary.add(new Judgement());
        CardLibrary.add(new JustLucky());
        CardLibrary.add(new LessonLearned());
        CardLibrary.add(new LikeWater());
        CardLibrary.add(new MasterReality());
        CardLibrary.add(new Meditate());
        CardLibrary.add(new MentalFortress());
        CardLibrary.add(new Nirvana());
        CardLibrary.add(new Omniscience());
        CardLibrary.add(new Perseverance());
        CardLibrary.add(new Pray());
        CardLibrary.add(new PressurePoints());
        CardLibrary.add(new Prostrate());
        CardLibrary.add(new Protect());
        CardLibrary.add(new Ragnarok());
        CardLibrary.add(new ReachHeaven());
        CardLibrary.add(new Rushdown());
        CardLibrary.add(new Sanctity());
        CardLibrary.add(new SandsOfTime());
        CardLibrary.add(new SashWhip());
        CardLibrary.add(new Scrawl());
        CardLibrary.add(new SignatureMove());
        CardLibrary.add(new SimmeringFury());
        CardLibrary.add(new SpiritShield());
        CardLibrary.add(new Strike_Purple());
        CardLibrary.add(new Study());
        CardLibrary.add(new Swivel());
        CardLibrary.add(new TalkToTheHand());
        CardLibrary.add(new Tantrum());
        CardLibrary.add(new ThirdEye());
        CardLibrary.add(new Tranquility());
        CardLibrary.add(new Vault());
        CardLibrary.add(new Vigilance());
        CardLibrary.add(new Wallop());
        CardLibrary.add(new WaveOfTheHand());
        CardLibrary.add(new Weave());
        CardLibrary.add(new WheelKick());
        CardLibrary.add(new WindmillStrike());
        CardLibrary.add(new Wish());
        CardLibrary.add(new Worship());
        CardLibrary.add(new WreathOfFlame());
    }

    private static void printMissingPortraitInfo() {
        AbstractCard card;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.jokePortrait != null) continue;
            System.out.println(card.name + ";" + card.color.name() + ";" + card.type.name());
        }
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (ImageMaster.loadImage("images/1024PortraitsBeta/" + card.assetUrl + ".png") != null) continue;
            System.out.println("[INFO] " + card.name + " missing LARGE beta portrait.");
        }
    }

    private static void printBlueCards(AbstractCard.CardColor color) {
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            if (c.getValue().color != color) continue;
            AbstractCard card = c.getValue();
            System.out.println(card.originalName + "; " + card.type.toString() + "; " + card.rarity.toString() + "; " + card.cost + "; " + card.rawDescription);
        }
    }

    private static void addColorlessCards() {
        CardLibrary.add(new Apotheosis());
        CardLibrary.add(new BandageUp());
        CardLibrary.add(new Blind());
        CardLibrary.add(new Chrysalis());
        CardLibrary.add(new DarkShackles());
        CardLibrary.add(new DeepBreath());
        CardLibrary.add(new Discovery());
        CardLibrary.add(new DramaticEntrance());
        CardLibrary.add(new Enlightenment());
        CardLibrary.add(new Finesse());
        CardLibrary.add(new FlashOfSteel());
        CardLibrary.add(new Forethought());
        CardLibrary.add(new GoodInstincts());
        CardLibrary.add(new HandOfGreed());
        CardLibrary.add(new Impatience());
        CardLibrary.add(new JackOfAllTrades());
        CardLibrary.add(new Madness());
        CardLibrary.add(new Magnetism());
        CardLibrary.add(new MasterOfStrategy());
        CardLibrary.add(new Mayhem());
        CardLibrary.add(new Metamorphosis());
        CardLibrary.add(new MindBlast());
        CardLibrary.add(new Panacea());
        CardLibrary.add(new Panache());
        CardLibrary.add(new PanicButton());
        CardLibrary.add(new Purity());
        CardLibrary.add(new SadisticNature());
        CardLibrary.add(new SecretTechnique());
        CardLibrary.add(new SecretWeapon());
        CardLibrary.add(new SwiftStrike());
        CardLibrary.add(new TheBomb());
        CardLibrary.add(new ThinkingAhead());
        CardLibrary.add(new Transmutation());
        CardLibrary.add(new Trip());
        CardLibrary.add(new Violence());
        CardLibrary.add(new Burn());
        CardLibrary.add(new Dazed());
        CardLibrary.add(new Slimed());
        CardLibrary.add(new VoidCard());
        CardLibrary.add(new Wound());
        CardLibrary.add(new Apparition());
        CardLibrary.add(new Beta());
        CardLibrary.add(new Bite());
        CardLibrary.add(new JAX());
        CardLibrary.add(new Insight());
        CardLibrary.add(new Miracle());
        CardLibrary.add(new Omega());
        CardLibrary.add(new RitualDagger());
        CardLibrary.add(new Safety());
        CardLibrary.add(new Shiv());
        CardLibrary.add(new Smite());
        CardLibrary.add(new ThroughViolence());
        CardLibrary.add(new BecomeAlmighty());
        CardLibrary.add(new FameAndFortune());
        CardLibrary.add(new LiveForever());
        CardLibrary.add(new Expunger());
    }

    private static void addCurseCards() {
        CardLibrary.add(new AscendersBane());
        CardLibrary.add(new CurseOfTheBell());
        CardLibrary.add(new Clumsy());
        CardLibrary.add(new Decay());
        CardLibrary.add(new Doubt());
        CardLibrary.add(new Injury());
        CardLibrary.add(new Necronomicurse());
        CardLibrary.add(new Normality());
        CardLibrary.add(new Pain());
        CardLibrary.add(new Parasite());
        CardLibrary.add(new Pride());
        CardLibrary.add(new Regret());
        CardLibrary.add(new Shame());
        CardLibrary.add(new Writhe());
    }

    private static void removeNonFinalizedCards() {
        ArrayList<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            if (c.getValue().assetUrl != null) continue;
            toRemove.add(c.getKey());
        }
        for (String s : toRemove) {
            logger.info("Removing Card " + s + " for trailer build.");
            cards.remove(s);
        }
        toRemove.clear();
        for (Map.Entry<String, AbstractCard> c : curses.entrySet()) {
            if (c.getValue().assetUrl != null) continue;
            toRemove.add(c.getKey());
        }
        for (String s : toRemove) {
            logger.info("Removing Curse " + s + " for trailer build.");
            curses.remove(s);
        }
    }

    public static void unlockAndSeeAllCards() {
        for (String string : UnlockTracker.lockedCards) {
            UnlockTracker.hardUnlockOverride(string);
        }
        for (Map.Entry entry : cards.entrySet()) {
            if (((AbstractCard)entry.getValue()).rarity == AbstractCard.CardRarity.BASIC || UnlockTracker.isCardSeen((String)entry.getKey())) continue;
            UnlockTracker.markCardAsSeen((String)entry.getKey());
        }
        for (Map.Entry entry : curses.entrySet()) {
            if (UnlockTracker.isCardSeen((String)entry.getKey())) continue;
            UnlockTracker.markCardAsSeen((String)entry.getKey());
        }
    }

    public static void add(AbstractCard card) {
        switch (card.color) {
            case RED: {
                ++redCards;
                if (!UnlockTracker.isCardSeen(card.cardID)) break;
                ++seenRedCards;
                break;
            }
            case GREEN: {
                ++greenCards;
                if (!UnlockTracker.isCardSeen(card.cardID)) break;
                ++seenGreenCards;
                break;
            }
            case PURPLE: {
                ++purpleCards;
                if (!UnlockTracker.isCardSeen(card.cardID)) break;
                ++seenPurpleCards;
                break;
            }
            case BLUE: {
                ++blueCards;
                if (!UnlockTracker.isCardSeen(card.cardID)) break;
                ++seenBlueCards;
                break;
            }
            case COLORLESS: {
                ++colorlessCards;
                if (!UnlockTracker.isCardSeen(card.cardID)) break;
                ++seenColorlessCards;
                break;
            }
            case CURSE: {
                ++curseCards;
                if (UnlockTracker.isCardSeen(card.cardID)) {
                    ++seenCurseCards;
                }
                curses.put(card.cardID, card);
                break;
            }
        }
        if (!UnlockTracker.isCardSeen(card.cardID)) {
            card.isSeen = false;
        }
        cards.put(card.cardID, card);
        ++totalCardCount;
    }

    public static AbstractCard getCopy(String key, int upgradeTime, int misc) {
        AbstractCard source = CardLibrary.getCard(key);
        AbstractCard retVal = null;
        retVal = source == null ? CardLibrary.getCard("Madness").makeCopy() : CardLibrary.getCard(key).makeCopy();
        for (int i = 0; i < upgradeTime; ++i) {
            retVal.upgrade();
        }
        retVal.misc = misc;
        if (misc != 0) {
            if (retVal.cardID.equals("Genetic Algorithm")) {
                retVal.block = misc;
                retVal.baseBlock = misc;
                retVal.initializeDescription();
            }
            if (retVal.cardID.equals("RitualDagger")) {
                retVal.damage = misc;
                retVal.baseDamage = misc;
                retVal.initializeDescription();
            }
        }
        return retVal;
    }

    public static AbstractCard getCopy(String key) {
        return CardLibrary.getCard(key).makeCopy();
    }

    public static AbstractCard getCard(AbstractPlayer.PlayerClass plyrClass, String key) {
        return cards.get(key);
    }

    public static AbstractCard getCard(String key) {
        return cards.get(key);
    }

    public static String getCardNameFromMetricID(String metricID) {
        String[] components = metricID.split("\\+");
        String baseId = components[0];
        AbstractCard card = cards.getOrDefault(baseId, null);
        if (card == null) {
            return metricID;
        }
        try {
            if (components.length > 1) {
                card = card.makeCopy();
                int upgrades = Integer.parseInt(components[1]);
                for (int i = 0; i < upgrades; ++i) {
                    card.upgrade();
                }
            }
        }
        catch (IndexOutOfBoundsException | NumberFormatException runtimeException) {
            // empty catch block
        }
        return card.name;
    }

    public static boolean isACard(String metricID) {
        String[] components = metricID.split("\\+");
        String baseId = components[0];
        AbstractCard card = cards.getOrDefault(baseId, null);
        return card != null;
    }

    public static AbstractCard getCurse() {
        ArrayList<String> tmp = new ArrayList<String>();
        for (Map.Entry<String, AbstractCard> c : curses.entrySet()) {
            if (c.getValue().cardID.equals("AscendersBane") || c.getValue().cardID.equals("Necronomicurse") || c.getValue().cardID.equals("CurseOfTheBell") || c.getValue().cardID.equals("Pride")) continue;
            tmp.add(c.getKey());
        }
        return cards.get(tmp.get(AbstractDungeon.cardRng.random(0, tmp.size() - 1)));
    }

    public static AbstractCard getCurse(AbstractCard prohibitedCard, Random rng) {
        ArrayList<String> tmp = new ArrayList<String>();
        for (Map.Entry<String, AbstractCard> c : curses.entrySet()) {
            if (Objects.equals(c.getValue().cardID, prohibitedCard.cardID) || Objects.equals(c.getValue().cardID, "Necronomicurse") || Objects.equals(c.getValue().cardID, "AscendersBane") || Objects.equals(c.getValue().cardID, "CurseOfTheBell") || Objects.equals(c.getValue().cardID, "Pride")) continue;
            tmp.add(c.getKey());
        }
        return cards.get(tmp.get(rng.random(0, tmp.size() - 1)));
    }

    public static AbstractCard getCurse(AbstractCard prohibitedCard) {
        return CardLibrary.getCurse(prohibitedCard, new Random());
    }

    public static void uploadCardData() {
        ArrayList<String> data = new ArrayList<String>();
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            data.add(c.getValue().gameDataUploadData());
            AbstractCard c2 = c.getValue().makeCopy();
            if (!c2.canUpgrade()) continue;
            c2.upgrade();
            data.add(c2.gameDataUploadData());
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.CARD_DATA, AbstractCard.gameDataUploadHeader(), data);
    }

    public static ArrayList<AbstractCard> getAllCards() {
        ArrayList<AbstractCard> retVal = new ArrayList<AbstractCard>();
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            retVal.add(c.getValue());
        }
        return retVal;
    }

    public static AbstractCard getAnyColorCard(AbstractCard.CardType type, AbstractCard.CardRarity rarity) {
        CardGroup anyCard = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            if (c.getValue().rarity != rarity || c.getValue().hasTag(AbstractCard.CardTags.HEALING) || c.getValue().type == AbstractCard.CardType.CURSE || c.getValue().type == AbstractCard.CardType.STATUS || c.getValue().type != type || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            anyCard.addToBottom(c.getValue());
        }
        anyCard.shuffle(AbstractDungeon.cardRandomRng);
        return anyCard.getRandomCard(true, rarity);
    }

    public static AbstractCard getAnyColorCard(AbstractCard.CardRarity rarity) {
        CardGroup anyCard = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            if (c.getValue().rarity != rarity || c.getValue().type == AbstractCard.CardType.CURSE || c.getValue().type == AbstractCard.CardType.STATUS || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            anyCard.addToBottom(c.getValue());
        }
        anyCard.shuffle(AbstractDungeon.cardRng);
        return anyCard.getRandomCard(true, rarity).makeCopy();
    }

    public static CardGroup getEachRare(AbstractPlayer p) {
        CardGroup everyRareCard = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            if (c.getValue().color != p.getCardColor() || c.getValue().rarity != AbstractCard.CardRarity.RARE) continue;
            everyRareCard.addToBottom(c.getValue().makeCopy());
        }
        return everyRareCard;
    }

    public static ArrayList<AbstractCard> getCardList(LibraryType type) {
        ArrayList<AbstractCard> retVal = new ArrayList<AbstractCard>();
        switch (type) {
            case COLORLESS: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.COLORLESS) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
            case CURSE: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.CURSE) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
            case RED: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.RED) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
            case GREEN: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.GREEN) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
            case BLUE: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.BLUE) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
            case PURPLE: {
                for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
                    if (c.getValue().color != AbstractCard.CardColor.PURPLE) continue;
                    retVal.add(c.getValue());
                }
                break;
            }
        }
        return retVal;
    }

    public static void addCardsIntoPool(ArrayList<AbstractCard> tmpPool, AbstractCard.CardColor color) {
        logger.info("[INFO] Adding " + (Object)((Object)color) + " cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != color || card.rarity == AbstractCard.CardRarity.BASIC || card.type == AbstractCard.CardType.STATUS || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static void addRedCards(ArrayList<AbstractCard> tmpPool) {
        logger.info("[INFO] Adding red cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != AbstractCard.CardColor.RED || card.rarity == AbstractCard.CardRarity.BASIC || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static void addGreenCards(ArrayList<AbstractCard> tmpPool) {
        logger.info("[INFO] Adding green cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != AbstractCard.CardColor.GREEN || card.rarity == AbstractCard.CardRarity.BASIC || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static void addBlueCards(ArrayList<AbstractCard> tmpPool) {
        logger.info("[INFO] Adding blue cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != AbstractCard.CardColor.BLUE || card.rarity == AbstractCard.CardRarity.BASIC || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static void addPurpleCards(ArrayList<AbstractCard> tmpPool) {
        logger.info("[INFO] Adding purple cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != AbstractCard.CardColor.PURPLE || card.rarity == AbstractCard.CardRarity.BASIC || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static void addColorlessCards(ArrayList<AbstractCard> tmpPool) {
        logger.info("[INFO] Adding colorless cards into card pool.");
        AbstractCard card = null;
        for (Map.Entry<String, AbstractCard> c : cards.entrySet()) {
            card = c.getValue();
            if (card.color != AbstractCard.CardColor.COLORLESS || card.type == AbstractCard.CardType.STATUS || UnlockTracker.isCardLocked(c.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            tmpPool.add(card);
        }
    }

    public static enum LibraryType {
        RED,
        GREEN,
        BLUE,
        PURPLE,
        CURSE,
        COLORLESS;

    }
}

