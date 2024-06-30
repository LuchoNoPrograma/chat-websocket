/**
 * axios setup to use mock service
 */

import axios from "axios";

const API_ENDPOINT = import.meta.env.VITE_BACKEND_URL;
const axiosServices = axios.create({
  baseURL: API_ENDPOINT
});

// interceptor for http
axiosServices.interceptors.response.use(
  (response) => response,
  (error) =>
    Promise.reject((error.response && error.response.data) || "Wrong Services")
);

export default axiosServices;
