<template>
  <div>
    <a-form-model ref="form" :model="model" :rules="validatorRules" class="password-retrieval-form" @keyup.enter.native="nextStep">

      <a-row :gutter="16">
        <a-col class="gutter-row" :span="20">
      <a-form-model-item v-if="show" label="邮箱" required prop="email" :labelCol="{span: 5}" :wrapperCol="{span: 19}">
            <a-input v-model="model.email" type="text" autocomplete="false" placeholder="请输入邮箱">
              <a-icon slot="prefix" type="mail" :style="{ color: 'rgba(0,0,0,.25)'}"/>
            </a-input>
      </a-form-model-item>
        </a-col>
      </a-row>

      <a-form-model-item v-if="show" required prop="actcode" label="验证码" :labelCol="{span: 5}" :wrapperCol="{span: 19}">
        <a-row :gutter="16">
          <a-col class="gutter-row" :span="12">
            <a-input v-model="model.actcode" type="text" placeholder="邮箱验证码">
              <a-icon slot="prefix" type="code" :style="{ color: 'rgba(0,0,0,.25)'}"/>
            </a-input>
          </a-col>

          <a-col class="gutter-row" :span="8">
            <a-button
              tabindex="-1"
              size="default"
              :disabled="state.smsSendBtn"
              @click.stop.prevent="getCaptcha"
              v-text="!state.smsSendBtn && '发送邮箱' || (state.time+' s')"></a-button>
          </a-col>
        </a-row>
      </a-form-model-item>

      <a-form-model-item :wrapperCol="{span: 19, offset: 5}">
        <router-link style="float: left;line-height: 40px;" :to="{ name: 'login' }">使用已有账户登录</router-link>
        <a-button type="primary" @click="nextStep" style="margin-left: 20px">下一步</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>

<script>
  import {postAction} from '@/api/manage'
  import {checkOnlyEmail} from "@api/api";

  export default {
    name: "Step2",
    props: ['userList'],
    data() {
      return {
        model: {},
        loading: false,
        // accountName: this.userList.username,
        dropList: "0",
        captcha: "",
        show: true,
        state: {
          time: 60,
          smsSendBtn: false,
        },
        formLogin: {
          actcode: "",
          email: "",
        },
        validatorRules: {
          email: [
            { required: true, message: '请输入邮箱!' },
            { validator: this.validateEmail }
          ],
          actcode: [
            { required: true, message: '请输入邮箱验证码!' }
          ]
        },
      }
    },
    computed: {
    },
    methods: {
      nextStep() {
        let that = this
        that.loading = true
        this.$refs['form'].validate((success) => {
          if(success==true){
            let params = {
              email: this.model.email,
              actcode: this.model.actcode
            }
            postAction("/sys/user/EmailVerification", params).then((res) => {
              if (res.success) {
                console.log(res);
                let userList = {
                  username: res.result.username,
                  email: params.email,
                  actcode: res.result.actcode
                };
                setTimeout(function () {
                  that.$emit('nextStep', userList)
                }, 0)
              } else {
                this.cmsFailed(res.message);
              }
            });
          }

        })
      },
      getCaptcha(e) {
        e.preventDefault();
        const that = this
        that.$refs['form'].validateField('email', err=>{
          if(!err){
            that.state.smsSendBtn = true;
            let interval = window.setInterval(() => {
              if (that.state.time-- <= 0) {
                that.state.time = 60;
                that.state.smsSendBtn = false;
                window.clearInterval(interval);
              }
            }, 1000);
            const hide = that.$message.loading('验证码发送中..', 0);
            let Params = {
              email: that.model.email,
              actcode: "2"
            };
            postAction("/sys/actcode", Params).then(res => {
              if (!res.success) {
                setTimeout(hide, 1);
                that.cmsFailed(res.message);
              }
              setTimeout(hide, 500);
            })
          }else{
            that.cmsFailed(err);
          }
        })
      },
      cmsFailed(err) {
        this.$notification['error']({
          message: "验证错误",
          description: err,
          duration: 4,
        });
      },
      handleChangeSelect(value) {
        var that = this;
        console.log(value);
        if (value == 0) {
          that.dropList = "0";
          that.show = true;
        } else {
          that.dropList = "1";
          that.show = false;
        }
      },
      validateEmail(rule, value, callback) {
        var reg= /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
        if(!reg.test(value)){
          callback(new Error("请输入正确邮箱"))
        }else{
          var params = {
            email: value,
          };
          checkOnlyEmail(params).then((res) => {
            if (res.success) {
              callback(邮箱不存在,请先注册)
            } else {
              callback()
            }
          })
        }
      }

    }

  }
</script>

<style lang="less" scoped>
  .stepFormText {
    margin-bottom: 24px;
  }

  .ant-form-item-label,
  .ant-form-item-control {
    line-height: 22px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }
</style>
