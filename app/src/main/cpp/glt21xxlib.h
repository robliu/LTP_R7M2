//
// Created by Rob on 4/17/2017.
//

#include <jni.h>

#ifndef LASERTRAININGPRO_GLT21XXLIB_H
#define LASERTRAININGPRO_GLT21XXLIB_H

#endif //LASERTRAININGPRO_GLT21XXLIB_H

#pragma once

int *  GetKeys(char *, int gModeCnt, int *modes);
int    GetThreshold(int, int, int);
int    GetThresholdLow(int, int, int);
int    GetThresholdLow_v2(int, int, int, int, int);
int    GetIPBK(int, int, int);
int    GetIPBK_v2(int, int, int, int, int);
double GetIntenseWeight(int, int, int);
