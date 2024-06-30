const AuthRoutes = {
  path: '/auth',
  component: () => import('@/layouts/blank/BlankLayout.vue'),
  meta: {
    requiresAuth: false
  },
  children: [
    {
      name: 'Boxed Register',
      path: '/auth/register',
      component: () => import('@/views/authentication/BoxedRegister.vue')
    },
    {
      name: 'Error',
      path: '/auth/404',
      component: () => import('@/views/authentication/Error.vue')
    },
    {
      name: 'Maintenance',
      path: '/auth/maintenance',
      component: () => import('@/views/authentication/Maintenance.vue')
    },
    // {
    //     name: "Material",
    //     path: "/icons/material",
    //     component: () => import("@/views/icons/MaterialIcons.vue"),
    //   },
    //   {
    //     name: "Feather",
    //     path: "/icons/tabler",
    //     component: () => import("@/views/icons/TablerIcons.vue"),
    //   },
  ]
};

export default AuthRoutes;
