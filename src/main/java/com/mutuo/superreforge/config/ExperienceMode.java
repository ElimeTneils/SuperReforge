package com.mutuo.superreforge.config;

/** 决定重铸经验成本使用玩家等级还是精确经验点。 */
public enum ExperienceMode {
    /** 按经验等级扣除，适合接近铁砧的原版体验。 */
    LEVELS,
    /** 按总经验点精确扣除，适合整合包细调经济。 */
    POINTS
}
