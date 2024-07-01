import ChatRoutes from "@/views/chat/ChatRoutes";

const PublicRoutes = {
  path: '/public',
  component: () => import('@/layouts/blank/BlankLayout.vue'),
  children: [
    ...ChatRoutes
  ]
};

export default PublicRoutes;